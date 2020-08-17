package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import akka.actor.ActorRef
import io.paytouch.core.async.monitors.{ ReceivingOrderChange, ReceivingOrderMonitor }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.ReceivingOrderConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.enums.{ CommentType, ReceivingOrderObjectType }
import io.paytouch.core.data.model.upsertions.ReceivingOrderUpsertion
import io.paytouch.core.data.model.{
  PurchaseOrderRecord,
  ReceivingOrderProductRecord,
  ReceivingOrderRecord,
  SlickRecord,
  ReceivingOrderUpdate => ReceivingOrderUpdateModel,
}
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{
  ReceivingOrder => ReceivingOrderEntity,
  ReceivingOrderCreation => ReceivingOrderCreationEntity,
  ReceivingOrderUpdate => ReceivingOrderUpdateEntity,
  _,
}
import io.paytouch.core.expansions.{ PurchaseOrderExpansions, ReceivingOrderExpansions, TransferOrderExpansions }
import io.paytouch.core.filters.ReceivingOrderFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.ReceivingOrderValidator
import io.paytouch.core.withTag
import io.paytouch.core.RichMap
import io.paytouch.core.utils.Multiple

import scala.concurrent._

class ReceivingOrderService(
    val eventTracker: ActorRef withTag EventTracker,
    val commentService: CommentService,
    val locationService: LocationService,
    val monitor: ActorRef withTag ReceivingOrderMonitor,
    purchaseOrderService: => PurchaseOrderService,
    receivingOrderProductService: => ReceivingOrderProductService,
    transferOrderService: => TransferOrderService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ReceivingOrderConversions
       with FindAllFeature
       with DeleteFeature
       with FindByIdFeature
       with CreateAndUpdateFeatureWithStateProcessing
       with CommentableFeature {

  type Creation = ReceivingOrderCreationEntity
  type Dao = ReceivingOrderDao
  type Entity = ReceivingOrderEntity
  type Expansions = ReceivingOrderExpansions
  type Filters = ReceivingOrderFilters
  type Model = ReceivingOrderUpsertion
  type Record = ReceivingOrderRecord
  type Update = ReceivingOrderUpdateEntity
  type Validator = ReceivingOrderValidator

  type State = (Record, Seq[ReceivingOrderProductRecord])

  protected val dao = daos.receivingOrderDao
  protected val validator = new ReceivingOrderValidator
  val defaultFilters = ReceivingOrderFilters()
  val commentType = CommentType.ReceivingOrder

  val classShortName = ExposedName.ReceivingOrder

  def enrich(
      receivingOrders: Seq[Record],
      filters: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val locationPerReceivingOrderR = getOptionalLocationPerReceivingOrder(receivingOrders)(e.withLocation)
    val userPerReceivingOrderR = getOptionalUserPerReceivingOrder(receivingOrders)(e.withUser)
    val purchaseOrderPerReceivingOrdersR =
      getOptionalPurchaseOrderPerReceivingOrder(receivingOrders)(e.withPurchaseOrder)
    val transferOrderPerReceivingOrdersR =
      getOptionalTransferOrderPerReceivingOrder(receivingOrders)(e.withTransferOrder)
    val productsCountPerReceivingOrderR =
      getOptionalProductsCountPerReceivingOrder(receivingOrders)(e.withProductsCount)
    val stockValuePerReceivingOrderR = getOptionalStockValuePerReceivingOrder(receivingOrders)(e.withStockValue)
    for {
      locationPerReceivingOrder <- locationPerReceivingOrderR
      userPerReceivingOrder <- userPerReceivingOrderR
      purchaseOrderPerReceivingOrders <- purchaseOrderPerReceivingOrdersR
      transferOrderPerReceivingOrders <- transferOrderPerReceivingOrdersR
      productsCountPerReceivingOrder <- productsCountPerReceivingOrderR
      stockValuePerReceivingOrder <- stockValuePerReceivingOrderR
    } yield fromRecordsAndOptionsToEntities(
      receivingOrders,
      locationPerReceivingOrder,
      userPerReceivingOrder,
      purchaseOrderPerReceivingOrders,
      transferOrderPerReceivingOrders,
      productsCountPerReceivingOrder,
      stockValuePerReceivingOrder,
    )
  }

  private def getOptionalLocationPerReceivingOrder(
      receivingOrders: Seq[Record],
    )(
      withLocation: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Location]] =
    getExpandedField[Location](
      locationService.findByIds,
      _.id,
      _.locationId,
      receivingOrders,
      withLocation,
    )

  private def getOptionalUserPerReceivingOrder(
      receivingOrders: Seq[Record],
    )(
      withUser: Boolean,
    ): Future[DataByRecord[UserInfo]] =
    getExpandedField[UserInfo](
      userService.getUserInfoByIds,
      _.id,
      _.userId,
      receivingOrders,
      withUser,
    )

  private def getOptionalProductsCountPerReceivingOrder(
      receivingOrders: Seq[Record],
    )(
      withProductsCount: Boolean,
    ): Future[DataByRecord[BigDecimal]] =
    if (withProductsCount)
      receivingOrderProductService.countProductsByReceivingOrderIds(receivingOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalStockValuePerReceivingOrder(
      receivingOrders: Seq[Record],
    )(
      withStockValue: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[MonetaryAmount]] =
    if (withStockValue)
      receivingOrderProductService.findStockValueByReceivingOrderIds(receivingOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalPurchaseOrderPerReceivingOrder(
      receivingOrders: Seq[Record],
    )(
      withPurchaseOrder: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[PurchaseOrder]] = {
    val purchaseOrderFinder: Seq[UUID] => Future[Seq[PurchaseOrder]] =
      purchaseOrderService.findByIds(_)(PurchaseOrderExpansions.withSupplier)
    val receivingOrdersWithPurchaseOrders =
      receivingOrders.filter(_.receivingObjectType.contains(ReceivingOrderObjectType.PurchaseOrder))

    getExpandedOptionalField[PurchaseOrder](
      purchaseOrderFinder,
      _.id,
      _.receivingObjectId,
      receivingOrdersWithPurchaseOrders,
      withPurchaseOrder,
    )
  }

  private def getOptionalTransferOrderPerReceivingOrder(
      receivingOrders: Seq[Record],
    )(
      withTransferOrder: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[TransferOrder]] = {
    val transferOrderFinder: Seq[UUID] => Future[Seq[TransferOrder]] =
      transferOrderService.findByIds(_)(TransferOrderExpansions())
    val receivingOrdersWithTransferOrders =
      receivingOrders.filter(_.receivingObjectType.contains(ReceivingOrderObjectType.Transfer))
    getExpandedOptionalField[TransferOrder](
      transferOrderFinder,
      _.id,
      _.receivingObjectId,
      receivingOrdersWithTransferOrders,
      withTransferOrder,
    )
  }

  def findPerReceivingObjectIdAndType(objectId: UUID, objectType: ReceivingOrderObjectType): Future[Seq[Record]] =
    dao.findByReceivingObjTypeAndIds(objectType, Seq(objectId)).map(_.getOrElse(objectId, Seq.empty))

  def findPerReceivingObjects[T <: SlickRecord](
      records: Seq[T],
      objectType: ReceivingOrderObjectType,
    )(implicit
      user: UserContext,
    ): Future[Map[T, Seq[Entity]]] =
    dao
      .findByReceivingObjTypeAndIds(objectType, records.map(_.id))
      .map(_.mapKeysToRecords(records).transform((_, v) => fromRecordsToEntities(v)))

  def countReceivedProductsByPurchaseOrderIds(
      purchaseOrders: Seq[PurchaseOrderRecord],
    ): Future[Map[PurchaseOrderRecord, BigDecimal]] =
    dao.countReceivedProductsByPurchaseOrderIds(purchaseOrders.map(_.id)).map(_.mapKeysToRecords(purchaseOrders))

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      receivingOrder <- convertToReceivingOrderUpdate(id, update)
      receivingOrderProducts <- receivingOrderProductService.convertToReceivingOrderProductUpdates(id, update)
    } yield Multiple.combine(receivingOrder, receivingOrderProducts)(ReceivingOrderUpsertion)

  private def convertToReceivingOrderUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ReceivingOrderUpdateModel]] =
    validator.validateUpsertion(id, update).mapNested(_ => fromUpsertionToUpdate(id, update))

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    receivingOrderProductService.findByReceivingOrderId(record.id).map(items => (record, items))

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.successful {
      monitor ! ReceivingOrderChange(state, entity, user)
    }

  def syncInventoryById(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator.validateSync(id).flatMapTraverse { record =>
      for {
        _ <- receivingOrderProductService.syncProductsByReceivingOrder(record)
        (resultType, updatedRecord) <- dao.markAsReceivedAndSynced(record.id)
        _ <- updateReceivingObjectStatus(updatedRecord)
        entity <- enrich(updatedRecord, defaultFilters)(ReceivingOrderExpansions())
      } yield (resultType, entity)
    }

  def updateReceivingObjectStatus(receivingOrderRecord: ReceivingOrderRecord)(implicit user: UserContext) = {
    val receivingObjectType = receivingOrderRecord.receivingObjectType
    val receivingObjectId = receivingOrderRecord.receivingObjectId
    (receivingObjectType, receivingObjectId) match {
      case (Some(ReceivingOrderObjectType.PurchaseOrder), Some(purchaseOrderId)) =>
        purchaseOrderService.inferAndUpdateStatuses(purchaseOrderId)
      case (Some(ReceivingOrderObjectType.Transfer), Some(transferOrderId)) =>
        transferOrderService.inferAndUpdateStatuses(transferOrderId)
      case _ => Future.unit
    }
  }

  override protected def validatedBulkDelete(ids: Seq[UUID])(implicit user: UserContext): Future[Unit] =
    for {
      toBeDeleted <- dao.findByIds(ids)
      _ <- super.validatedBulkDelete(ids)
      _ <- Future.sequence(toBeDeleted.map(updateReceivingObjectStatus))
    } yield ()
}
