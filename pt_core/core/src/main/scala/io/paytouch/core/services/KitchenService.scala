package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.KitchenConversions
import io.paytouch.core.data.daos.{ Daos, KitchenDao }
import io.paytouch.core.data.model.{ KitchenRecord, KitchenUpdate => KitchenUpdateModel }
import io.paytouch.core.entities.enums.{ ExposedName, MerchantSetupSteps }
import io.paytouch.core.entities.{
  KitchenCreation,
  Pagination,
  UserContext,
  Kitchen => KitchenEntity,
  KitchenUpdate => KitchenUpdateEntity,
}
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.KitchenFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.utils.Tagging.withTag
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.KitchenValidator

import scala.concurrent._

class KitchenService(
    val eventTracker: ActorRef withTag EventTracker,
    val setupStepService: SetupStepService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends KitchenConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeatureWithStateProcessing
       with SoftDeleteFeature {

  type Creation = KitchenCreation
  type Dao = KitchenDao
  type Entity = KitchenEntity
  type Expansions = NoExpansions
  type Filters = KitchenFilters
  type Model = KitchenUpdateModel
  type Record = KitchenRecord
  type State = Unit
  type Update = KitchenUpdateEntity
  type Validator = KitchenValidator

  val classShortName = ExposedName.Kitchen
  val defaultFilters = KitchenFilters()
  val defaultExpansions = NoExpansions()

  protected val dao = daos.kitchenDao
  protected val validator = new KitchenValidator

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    Future.successful(toSeqEntity(records))

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Model]] =
    validator.validateUpsertion(id, update).mapNested(_ => fromUpsertionToUpdate(id, update))

  def getKitchensMap()(implicit user: UserContext): Future[Map[UUID, Entity]] =
    findAll(KitchenFilters.withAccessibleLocations())(defaultExpansions)(user, Pagination(1, 100)).map {
      case (items, _) => items.map(k => (k.id, k)).toMap
    }

  def findByIds(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] = dao.findByIds(ids)

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.unit

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ) =
    setupStepService.checkStepCompletion(entity, MerchantSetupSteps.SetupKitchens)
}
