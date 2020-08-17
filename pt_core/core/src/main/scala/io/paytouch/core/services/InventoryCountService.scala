package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import akka.actor.ActorRef

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.InventoryCountConversions
import io.paytouch.core.data.daos.{ Daos, InventoryCountDao }
import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.data.model.upsertions.InventoryCountUpsertion
import io.paytouch.core.data.model.{ InventoryCountRecord, InventoryCountUpdate => InventoryCountUpdateModel }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{
  Location,
  UserContext,
  UserInfo,
  InventoryCount => InventoryCountEntity,
  InventoryCountCreation => InventoryCountCreationEntity,
  InventoryCountUpdate => InventoryCountUpdateEntity,
}
import io.paytouch.core.expansions.InventoryCountExpansions
import io.paytouch.core.filters.InventoryCountFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.InventoryCountValidator
import io.paytouch.core.withTag

import scala.concurrent._

class InventoryCountService(
    val commentService: CommentService,
    val eventTracker: ActorRef withTag EventTracker,
    val locationService: LocationService,
    inventoryCountProductService: => InventoryCountProductService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends InventoryCountConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeature
       with CommentableFeature
       with DeleteFeature {

  type Creation = InventoryCountCreationEntity
  type Dao = InventoryCountDao
  type Entity = InventoryCountEntity
  type Expansions = InventoryCountExpansions
  type Filters = InventoryCountFilters
  type Model = InventoryCountUpsertion
  type Record = InventoryCountRecord
  type Update = InventoryCountUpdateEntity
  type Validator = InventoryCountValidator

  val defaultFilters = InventoryCountFilters()
  protected val validator = new InventoryCountValidator
  val commentType = CommentType.InventoryCount
  val classShortName = ExposedName.InventoryCount

  protected val dao = daos.inventoryCountDao

  override def validateUpdate(id: UUID)(implicit user: UserContext) = validator.validateUpdate(id)

  def enrich(
      inventoryCounts: Seq[Record],
      f: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val productsCountPerInventoryCountR = getProductsCount(inventoryCounts)
    val userPerInventoryCountR = getOptionalUser(inventoryCounts)(e.withUser)
    val locationPerInventoryCountR = getOptionalLocation(inventoryCounts)(e.withLocation)
    for {
      productsCounts <- productsCountPerInventoryCountR
      users <- userPerInventoryCountR
      locations <- locationPerInventoryCountR
    } yield fromRecordsAndOptionsToEntities(inventoryCounts, productsCounts, users, locations)
  }

  private def getProductsCount(inventoryCounts: Seq[Record]): Future[Map[Record, Int]] =
    inventoryCountProductService.countProductsByInventoryCountIds(inventoryCounts)

  private def getOptionalUser(inventoryCounts: Seq[Record])(withUser: Boolean): Future[DataByRecord[UserInfo]] =
    getExpandedField[UserInfo](
      userService.getUserInfoByIds,
      _.id,
      _.userId,
      inventoryCounts,
      withUser,
    )

  private def getOptionalLocation(
      inventoryCounts: Seq[Record],
    )(
      withLocation: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Location]] =
    getExpandedField[Location](
      locationService.findByIds,
      _.id,
      _.locationId,
      inventoryCounts,
      withLocation,
    )

  def findByIds(ids: Seq[UUID])(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      records <- dao.findByIds(ids)
      entities <- enrich(records, defaultFilters)(e)
    } yield entities

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      inventoryCount <- convertToInventoryCountUpdate(id, update)
      inventoryCountProducts <- inventoryCountProductService.convertToInventoryCountProductUpdates(id, update)
    } yield Multiple.combine(inventoryCount, inventoryCountProducts)(InventoryCountUpsertion)

  private def convertToInventoryCountUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[InventoryCountUpdateModel]] =
    validator.validateUpsertion(update).mapNested(_ => fromUpsertionToUpdate(id, update))

  def syncInventoryById(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator.validateUpdate(id).flatMapTraverse { record =>
      for {
        inferredStatus <- inventoryCountProductService.syncProductsByInventoryCount(record)
        (resultType, updatedRecord) <- dao.markAsSyncedAndUpdateStatus(record.id, inferredStatus)
        entity <- enrich(updatedRecord, defaultFilters)(InventoryCountExpansions.empty)
      } yield (resultType, entity)
    }

}
