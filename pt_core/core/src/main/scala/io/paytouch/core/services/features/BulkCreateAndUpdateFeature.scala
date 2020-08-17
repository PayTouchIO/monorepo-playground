package io.paytouch.core.services.features

import cats.implicits._

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.model.{ SlickMerchantRecord, SlickUpdate }
import io.paytouch.core.entities._
import io.paytouch.core.utils.Implicits
import io.paytouch.core.utils._

import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.{ ValidatorWithExtraFields, ValidatorWithRelIds }

import scala.concurrent._

trait BulkCreateFeature extends BulkInsertOrUpdateFeature {
  type Creation = CreationEntityWithRelIds[Entity, Update]

  def bulkCreate(creations: Seq[Creation])(implicit user: UserContext): Future[ErrorsOr[Result[Seq[Entity]]]] =
    convertAndUpsert(creations.map(_.asUpdate))
}

trait BulkUpdateFeature extends BulkUpdateFeatureWithStateProcessing {
  type State = Seq[Record]

  protected def saveCurrentState(records: Seq[Record])(implicit user: UserContext): Future[State] =
    Future.successful(records)
  protected def processChangeOfState(
      state: State,
      updates: Seq[Update],
      resultType: ResultType,
      entities: Seq[Entity],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.unit
}

trait BulkUpdateFeatureWithStateProcessing extends BulkInsertOrUpdateFeature { self =>
  type State
  type Validator <: ValidatorWithRelIds[Record]

  protected def saveCurrentState(records: Seq[Record])(implicit user: UserContext): Future[State]
  protected def processChangeOfState(
      state: State,
      updates: Seq[Update],
      resultType: ResultType,
      entities: Seq[Entity],
    )(implicit
      user: UserContext,
    ): Future[Unit]

  def bulkUpdate(updates: Seq[Update])(implicit user: UserContext): Future[ErrorsOr[Result[Seq[Entity]]]] = {
    val relIds = updates.map(upd => (upd.relId1, upd.relId2))
    for {
      records <- validator.getValidByRelIds(relIds)
      currentState <- saveCurrentState(records)
      upsertionResult <- convertAndUpsert(updates)
    } yield upsertionResult.map {
      case (resultType, entities) =>
        processChangeOfState(currentState, updates, resultType, entities)
        (resultType, entities)
    }
  }
}

trait BulkCreateAndUpdateFeatureWithStateProcessing extends BulkCreateFeature with BulkUpdateFeatureWithStateProcessing

trait BulkCreateAndUpdateFeature extends BulkCreateFeature with BulkUpdateFeature

trait BulkInsertOrUpdateFeature extends Implicits { self =>
  type Dao <: SlickDao { type Record = self.Record; type Update = self.Model }
  type Entity <: ExposedEntity
  type Model <: SlickUpdate[Record]
  type Record <: SlickMerchantRecord
  type Update <: UpdateEntityWithRelIds[Entity]
  type Validator <: ValidatorWithExtraFields[Record]

  protected def dao: Dao
  protected def validator: Validator

  implicit def toFutureResultTypeEntities(
      f: Future[(ResultType, Seq[Record])],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Seq[Entity])]

  def bulkUpsert(upsertionModel: Seq[Model])(implicit user: UserContext): Future[(ResultType, Seq[Record])] =
    dao.bulkUpsert(upsertionModel).map { results =>
      val resultTypes = results.map { case (rt, _) => rt }
      val overallResultType = resultTypes.find(_ == ResultType.Created).getOrElse(ResultType.Updated)
      val entities = results.map { case (_, e) => e }
      (overallResultType, entities)
    }

  protected def convertToUpsertionModel(
      updates: Seq[Update],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Model]]]

  def convertAndUpsert(updates: Seq[Update])(implicit user: UserContext): Future[ErrorsOr[Result[Seq[Entity]]]] =
    convertToUpsertionModel(updates).flatMapTraverse(upsertionModel => bulkUpsert(upsertionModel))
}
