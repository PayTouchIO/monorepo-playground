package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.ReturnOrderConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.data.model.upsertions.ReturnOrderUpsertion
import io.paytouch.core.data.model.{
  PurchaseOrderRecord,
  ReturnOrderRecord,
  ReturnOrderUpdate => ReturnOrderUpdateModel,
}
import io.paytouch.core.entities.{
  Location,
  MonetaryAmount,
  PurchaseOrder,
  ReturnOrderCreation,
  Supplier,
  UserContext,
  UserInfo,
  ReturnOrder => ReturnOrderEntity,
  ReturnOrderUpdate => ReturnOrderUpdateEntity,
}
import io.paytouch.core.expansions.{ PurchaseOrderExpansions, ReturnOrderExpansions }
import io.paytouch.core.filters.ReturnOrderFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.ReturnOrderValidator
import io.paytouch.core.RichMap
import io.paytouch.core.utils.Multiple

import scala.concurrent._

class ReturnOrderService(
    val commentService: CommentService,
    val locationService: LocationService,
    returnOrderProductService: => ReturnOrderProductService,
    val supplierService: SupplierService,
    val userService: UserService,
    purchaseOrderService: => PurchaseOrderService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ReturnOrderConversions
       with CommentableFeature
       with FindByIdFeature
       with FindAllFeature
       with CreateAndUpdateFeature {

  type Creation = ReturnOrderCreation
  type Dao = ReturnOrderDao
  type Entity = ReturnOrderEntity
  type Expansions = ReturnOrderExpansions
  type Filters = ReturnOrderFilters
  type Model = ReturnOrderUpsertion
  type Record = ReturnOrderRecord
  type Update = ReturnOrderUpdateEntity
  type Validator = ReturnOrderValidator

  protected val validator = new ReturnOrderValidator
  val commentType = CommentType.ReturnOrder

  protected val dao = daos.returnOrderDao

  val defaultFilters = ReturnOrderFilters()

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val userPerRecordR = getOptionalUserInfoPerReturnOrder(records)(e.withUser)
    val locationPerReturnOrderR = getOptionalLocation(records)(e.withLocation)
    val productCountPerRecordR = getOptionalProductsCountPerReturnOrder(records)(e.withProductsCount)
    val supplierPerRecordR = getOptionalSupplierPerReturnOrder(records)(e.withSupplier)
    val stockValuePerRecordR = getOptionalStockValuePerReturnOrder(records)(e.withStockValue)
    val purchaseOrderPerRecordR =
      getOptionalPurchaseOrderPerReturnOrder(records)(e.withPurchaseOrder)
    for {
      locationPerReturnOrder <- locationPerReturnOrderR
      userPerRecord <- userPerRecordR
      productCountPerRecord <- productCountPerRecordR
      supplierPerRecord <- supplierPerRecordR
      stockValuePerRecord <- stockValuePerRecordR
      purchaseOrderPerRecord <- purchaseOrderPerRecordR
    } yield fromRecordsAndOptionsToEntities(
      records,
      locationPerReturnOrder,
      purchaseOrderPerRecord,
      productCountPerRecord,
      userPerRecord,
      supplierPerRecord,
      stockValuePerRecord,
    )
  }

  def countReturnedProductsByPurchaseOrderIds(
      purchaseOrders: Seq[PurchaseOrderRecord],
    ): Future[Map[PurchaseOrderRecord, BigDecimal]] =
    dao
      .countReturnedProductsByPurchaseOrderIds(purchaseOrders.map(_.id))
      .map(_.mapKeysToRecords(purchaseOrders))

  private def getOptionalUserInfoPerReturnOrder(
      returnOrders: Seq[Record],
    )(
      withUser: Boolean,
    ): Future[DataByRecord[UserInfo]] =
    getExpandedField[UserInfo](
      userService.getUserInfoByIds,
      _.id,
      _.userId,
      returnOrders,
      withUser,
    )

  private def getOptionalLocation(
      returnOrders: Seq[Record],
    )(
      withLocation: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Location]] =
    getExpandedField[Location](
      locationService.findByIds,
      _.id,
      _.locationId,
      returnOrders,
      withLocation,
    )

  private def getOptionalSupplierPerReturnOrder(
      returnOrders: Seq[Record],
    )(
      withSupplier: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Supplier]] =
    getExpandedField[Supplier](
      supplierService.findByIds,
      _.id,
      _.supplierId,
      returnOrders,
      withSupplier,
    )

  private def getOptionalStockValuePerReturnOrder(
      returnOrders: Seq[Record],
    )(
      withStockValue: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[MonetaryAmount]] =
    if (withStockValue)
      returnOrderProductService.findStockValueByReturnOrders(returnOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalProductsCountPerReturnOrder(
      returnOrders: Seq[Record],
    )(
      withProductsCount: Boolean,
    ): Future[DataByRecord[BigDecimal]] =
    if (withProductsCount)
      returnOrderProductService.countProductsByReturnOrderIds(returnOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalPurchaseOrderPerReturnOrder(
      returnOrders: Seq[Record],
    )(
      withPurchaseOrder: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[PurchaseOrder]] = {
    val purchaseOrderFinder: Seq[UUID] => Future[Seq[PurchaseOrder]] =
      purchaseOrderService.findByIds(_)(PurchaseOrderExpansions.withSupplier)

    getExpandedOptionalField[PurchaseOrder](
      purchaseOrderFinder,
      _.id,
      _.purchaseOrderId,
      returnOrders,
      withPurchaseOrder,
    )
  }

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      returnOrder <- convertToReturnOrderUpdateModel(id, update)
      returnOrderProducts <- returnOrderProductService.convertToReturnOrderProductUpdates(id, update)
    } yield Multiple.combine(returnOrder, returnOrderProducts)(ReturnOrderUpsertion)

  private def convertToReturnOrderUpdateModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ReturnOrderUpdateModel]] =
    validator.validateUpsertion(update).mapNested(_ => fromUpsertionToUpdate(id, update))

  def syncInventoryById(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator.validateUpdate(id).flatMapTraverse { record =>
      for {
        _ <- returnOrderProductService.syncProductsByReceivingOrder(record)
        (resultType, updatedRecord) <- dao.markAsReturnedAndSynced(record.id)
        entity <- enrich(updatedRecord, defaultFilters)(ReturnOrderExpansions())
      } yield (resultType, entity)
    }
}
