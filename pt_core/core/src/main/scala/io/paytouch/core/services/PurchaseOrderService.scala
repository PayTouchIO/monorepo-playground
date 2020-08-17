package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.PurchaseOrderConversions
import io.paytouch.core.data.daos.{ Daos, PurchaseOrderDao }
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.upsertions.PurchaseOrderUpsertion
import io.paytouch.core.data.model.{
  PurchaseOrderRecord,
  ReceivingOrderRecord,
  PurchaseOrderUpdate => PurchaseOrderUpdateModel,
}
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{
  Location,
  ReceivingOrder,
  Supplier,
  UserContext,
  UserInfo,
  PurchaseOrder => PurchaseOrderEntity,
  PurchaseOrderCreation => PurchaseOrderCreationEntity,
  PurchaseOrderUpdate => PurchaseOrderUpdateEntity,
}
import io.paytouch.core.expansions.PurchaseOrderExpansions
import io.paytouch.core.filters.PurchaseOrderFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.PurchaseOrderValidator
import io.paytouch.core.withTag

import scala.concurrent._

class PurchaseOrderService(
    val commentService: CommentService,
    val eventTracker: ActorRef withTag EventTracker,
    val locationService: LocationService,
    val purchaseOrderProductService: PurchaseOrderProductService,
    val receivingOrderService: ReceivingOrderService,
    val returnOrderService: ReturnOrderService,
    val supplierService: SupplierService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends PurchaseOrderConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeature
       with CommentableFeature
       with SoftDeleteFeature
       with ReceivingObjectUpdateService {

  type Creation = PurchaseOrderCreationEntity
  type Dao = PurchaseOrderDao
  type Entity = PurchaseOrderEntity
  type Expansions = PurchaseOrderExpansions
  type Filters = PurchaseOrderFilters
  type Model = PurchaseOrderUpsertion
  type Record = PurchaseOrderRecord
  type Update = PurchaseOrderUpdateEntity
  type Validator = PurchaseOrderValidator

  val classShortName = ExposedName.PurchaseOrder
  val defaultFilters = PurchaseOrderFilters()
  protected val validator = new PurchaseOrderValidator
  val commentType = CommentType.PurchaseOrder

  protected val dao = daos.purchaseOrderDao
  val objectType = ReceivingOrderObjectType.PurchaseOrder

  def enrich(
      purchaseOrders: Seq[Record],
      f: Filters,
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val supplierPerPurchaseOrderR = getOptionalSupplier(purchaseOrders)(e.withSupplier)
    val locationPerPurchaseOrderR = getOptionalLocation(purchaseOrders)(e.withLocation)
    val userPerPurchaseOrderR = getOptionalUser(purchaseOrders)(e.withUser)
    val receivingOrderPerPurchaseOrderR = getOptionalReceivingOrders(purchaseOrders)(e.withReceivingOrders)
    val orderProductsCountPerPurchaseOrderR =
      getOptionalOrderedProductCount(purchaseOrders)(e.withOrderedProductsCount)
    val receivedProductsCountPerPurchaseOrderR =
      getOptionalReceivedProductsCount(purchaseOrders)(e.withReceivedProductsCount)
    val returnedProductsCountPerPurchaseOrderR =
      getOptionalReturnedProductsCount(purchaseOrders)(e.withReturnedProductsCount)
    for {
      suppliers <- supplierPerPurchaseOrderR
      locations <- locationPerPurchaseOrderR
      users <- userPerPurchaseOrderR
      receivingOrderPerPurchaseOrder <- receivingOrderPerPurchaseOrderR
      orderProductsCountPerPurchaseOrder <- orderProductsCountPerPurchaseOrderR
      receivedProductsCountPerPurchaseOrder <- receivedProductsCountPerPurchaseOrderR
      returnedProductsCountPerPurchaseOrder <- returnedProductsCountPerPurchaseOrderR
    } yield fromRecordsAndOptionsToEntities(
      purchaseOrders,
      suppliers,
      locations,
      users,
      receivingOrderPerPurchaseOrder,
      orderProductsCountPerPurchaseOrder,
      receivedProductsCountPerPurchaseOrder,
      returnedProductsCountPerPurchaseOrder,
    )
  }

  private def getOptionalSupplier(
      purchaseOrders: Seq[Record],
    )(
      withSupplier: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Supplier]] =
    getExpandedField[Supplier](
      supplierService.findByIds,
      _.id,
      _.supplierId,
      purchaseOrders,
      withSupplier,
    )

  private def getOptionalLocation(
      purchaseOrders: Seq[Record],
    )(
      withLocation: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Location]] =
    getExpandedField[Location](
      locationService.findByIds,
      _.id,
      _.locationId,
      purchaseOrders,
      withLocation,
    )

  private def getOptionalUser(purchaseOrders: Seq[Record])(withUser: Boolean): Future[DataByRecord[UserInfo]] =
    getExpandedField[UserInfo](
      userService.getUserInfoByIds,
      _.id,
      _.userId,
      purchaseOrders,
      withUser,
    )

  private def getOptionalReceivingOrders(
      purchaseOrders: Seq[Record],
    )(
      withReceivingOrders: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[ReceivingOrder]] =
    if (withReceivingOrders) {
      val objectType = ReceivingOrderObjectType.PurchaseOrder
      receivingOrderService.findPerReceivingObjects(purchaseOrders, objectType).map(Some(_))
    }
    else Future.successful(None)

  private def getOptionalOrderedProductCount(
      purchaseOrders: Seq[Record],
    )(
      withOrderProductsCount: Boolean,
    ): Future[DataByRecord[BigDecimal]] =
    if (withOrderProductsCount)
      purchaseOrderProductService.countOrderedProductsByPurchaseOrderIds(purchaseOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalReceivedProductsCount(
      purchaseOrders: Seq[Record],
    )(
      withReceivedProductsCount: Boolean,
    ): Future[DataByRecord[BigDecimal]] =
    if (withReceivedProductsCount)
      receivingOrderService.countReceivedProductsByPurchaseOrderIds(purchaseOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalReturnedProductsCount(
      purchaseOrders: Seq[Record],
    )(
      withReturnedProductsCount: Boolean,
    ): Future[DataByRecord[BigDecimal]] =
    if (withReturnedProductsCount)
      returnOrderService.countReturnedProductsByPurchaseOrderIds(purchaseOrders).map(Some(_))
    else Future.successful(None)

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
      purchaseOrder <- convertToPurchaseOrderUpdate(id, update)
      purchaseOrderProducts <- purchaseOrderProductService.convertToPurchaseOrderProductUpdates(id, update)
    } yield Multiple.combine(purchaseOrder, purchaseOrderProducts)(PurchaseOrderUpsertion)

  private def convertToPurchaseOrderUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[PurchaseOrderUpdateModel]] =
    validator.validateUpsertion(id, update).mapNested(_ => fromUpsertionToUpdate(id, update))

  protected def updatePaymentStatus(
      objectRecord: Dao#Record,
      receivingOrders: Seq[ReceivingOrderRecord],
    ): Future[Int] = {
    val status = receivingOrders match {
      case orders if orders.nonEmpty && orders.forall(_.paymentStatus.contains(ReceivingOrderPaymentStatus.Paid)) =>
        PurchaseOrderPaymentStatus.Paid
      case orders if orders.nonEmpty && orders.exists(_.paymentStatus.contains(ReceivingOrderPaymentStatus.Paid)) =>
        PurchaseOrderPaymentStatus.Partial
      case _ => PurchaseOrderPaymentStatus.Unpaid
    }
    dao.setPaymentStatus(objectRecord.id, status)
  }

  def send(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator.validateSend(id).flatMapTraverse { record =>
      for {
        (resultType, updatedRecord) <- dao.markAsSent(record.id)
        entity <- enrich(updatedRecord, defaultFilters)(PurchaseOrderExpansions())
      } yield (resultType, entity)
    }
}
