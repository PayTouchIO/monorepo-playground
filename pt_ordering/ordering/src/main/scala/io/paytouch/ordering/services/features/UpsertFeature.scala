package io.paytouch.ordering.services.features

import java.util.UUID

import scala.concurrent.Future

import cats.data._

import io.paytouch.ordering.{ Result, UpsertionResult }
import io.paytouch.ordering.data.daos.features.SlickUpsertDao
import io.paytouch.ordering.data.model.upsertions.UpsertionModel
import io.paytouch.ordering.entities.{ AppContext, CreationEntity, ExposedEntity, UpdateEntity }
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.features.{ UpsertionValidator, ValidatorWithExtraFields }

trait CreateFeature extends UpsertFeature {
  type Creation <: CreationEntity[Upsertion]

  def create(id: UUID, creation: Creation)(implicit context: Context): Future[UpsertionResult[Entity]] =
    validator.availableOneById(id).flatMap {
      case Validated.Valid(_)       => convertAndUpsert(id, creation.asUpsert, None)
      case i @ Validated.Invalid(_) => Future.successful(i)
    }
}

trait StandardUpdateFeature extends GenericUpdateFeature {
  type Update = Upsertion

  def asUpsert(update: Update): Upsertion = update
}

trait UpdateFeature extends GenericUpdateFeature {
  type Update <: UpdateEntity[Upsertion]

  def asUpsert(update: Update): Upsertion = update.asUpsert
}

trait GenericUpdateFeature extends UpsertFeature {
  type Update

  def update(id: UUID, update: Update)(implicit context: Context): Future[UpsertionResult[Entity]] =
    validateUpdate(id).flatMap {
      case Validated.Valid(existing) =>
        convertAndUpsert(id, asUpsert(update), Some(existing))

      case i @ Validated.Invalid(_) =>
        Future.successful(i)
    }

  protected def validateUpdate(id: UUID)(implicit context: Context) =
    validator.accessOneById(id)

  protected def asUpsert(update: Update): Upsertion
}

trait UpsertFeature extends EnrichFeature { self =>
  type Context <: AppContext
  type Dao <: SlickUpsertDao { type Record = self.Record; type Upsertion = self.Model }
  type Entity <: ExposedEntity
  type Model <: UpsertionModel[Record]
  type Upsertion
  type Validator <: ValidatorWithExtraFields with UpsertionValidator {
    type Context = self.Context; type Record = self.Record; type Upsertion = self.Upsertion
  }

  protected def dao: Dao
  protected def validator: Validator

  protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[Model]

  protected def validateUpsertionModel(
      id: UUID,
      model: Model,
    )(implicit
      context: Context,
    ): Future[ValidatedData[Model]] =
    Future.successful(ValidatedData.success(model))

  protected def convertAndUpsert(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      context: Context,
    ): Future[UpsertionResult[Entity]] = {
    val validatedModel = for {
      validUpsertion <- validator.validateUpsertion(id, upsertion, existing)
      model <- convertToUpsertionModel(id, upsertion, existing)
      validModel <- validateUpsertionModel(id, model)
    } yield ValidatedData.combine(validUpsertion, validModel) { case (_, m) => m }

    validatedModel.flatMapValid(upsert(_, existing))
  }

  protected def upsert(model: Model, existing: Option[Record])(implicit context: Context): Future[Result[Entity]] =
    for {
      (resultType, record) <- dao.upsert(model)
      entity <- enrich(record)
      _ <- processAfterUpsert(entity, existing)
    } yield resultType -> entity

  protected def processAfterUpsert(
      currentEntity: Entity,
      previousRecord: Option[Record],
    )(implicit
      context: Context,
    ): Future[Unit] =
    Future.unit
}
