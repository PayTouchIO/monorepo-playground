package io.paytouch.core.services.features

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.data.daos.features.SlickUpsertDao
import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.data.model.upsertions.UpsertionModel
import io.paytouch.core.entities._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.features.ValidatorWithExtraFields

trait CreateFeature extends CreateFeatureWithStateProcessing {
  type State = AnyRef

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.unit
}

trait CreateFeatureWithStateProcessing extends InsertOrUpdateFeature {

  type Creation <: CreationEntity[Entity, Update]
  type State

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]]

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit]

  def create(id: UUID, creation: Creation)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator.availableOneById(id).flatMap {
      case Validated.Valid(_) =>
        for {
          currentState <- saveCreationState(id, creation)
          creationResult <- convertAndUpsert(id, creation.asUpdate)
        } yield creationResult.map {
          case (resultType, entity) =>
            processChangeOfState(currentState, creation.asUpdate, resultType, entity)
            (resultType, entity)
        }

      case i @ Validated.Invalid(_) =>
        Future.successful(i)
    }
}

trait UpdateFeature extends UpdateFeatureWithStateProcessing {
  type State = AnyRef

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.successful(record)

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.unit
}

trait UpdateFeatureWithStateProcessing extends InsertOrUpdateFeature {
  type State

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State]

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit]

  def update(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validateUpdate(id).flatMap {
      case Validated.Valid(record) =>
        for {
          currentState <- saveCurrentState(record)
          upsertionResult <- convertAndUpsert(id, update)
        } yield upsertionResult.map {
          case (resultType, entity) =>
            processChangeOfState(Some(currentState), update, resultType, entity)

            (resultType, entity)
        }

      case i @ Validated.Invalid(_) =>
        Future.successful(i)
    }

  protected def convertAndUpdate(id: UUID, update: Update)(implicit user: UserContext) =
    convertAndUpsert(id, update)

  def validateUpdate(id: UUID)(implicit user: UserContext) =
    validator.accessOneById(id)
}

trait CreateAndUpdateFeatureWithStateProcessing
    extends CreateFeatureWithStateProcessing
       with UpdateFeatureWithStateProcessing

trait CreateAndUpdateFeature extends CreateFeature with UpdateFeature {
  override type State = AnyRef

  override def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.unit
}

trait InsertOrUpdateFeature extends Implicits { self =>
  type Dao <: SlickUpsertDao { type Record = self.Record; type Upsertion = self.Model }
  type Entity <: ExposedEntity
  type Model <: UpsertionModel[Record]
  type Record <: SlickMerchantRecord
  type Update <: UpdateEntity[Entity]
  type Validator <: ValidatorWithExtraFields[Record]

  protected def dao: Dao
  protected def validator: Validator

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)]

  final def upsert(id: UUID, upsertionModel: Model)(implicit user: UserContext): Future[(ResultType, Record)] =
    dao.upsert(upsertionModel)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]]

  final def convertAndUpsert(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    convertToUpsertionModel(id, update).flatMapTraverse(upsert(id, _))
}
