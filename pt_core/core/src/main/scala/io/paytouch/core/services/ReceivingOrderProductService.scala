package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.RichMap
import io.paytouch.core.conversions.ReceivingOrderProductConversions
import io.paytouch.core.data.daos.{ Daos, ReceivingOrderProductDao }
import io.paytouch.core.data.model.enums.{ QuantityChangeReason, ReceivingOrderObjectType }
import io.paytouch.core.data.model.{
  ReceivingOrderProductRecord,
  ReceivingOrderProductUpdate,
  ReceivingOrderRecord,
  StockRecord,
}
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{
  ReceivingOrderExpansions,
  ReceivingOrderProductExpansions,
  TransferOrderExpansions,
}
import io.paytouch.core.filters.ReceivingOrderProductFilters
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ArticleValidator

class ReceivingOrderProductService(
    val articleService: ArticleService,
    purchaseOrderProductService: => PurchaseOrderProductService,
    receivingOrderService: => ReceivingOrderService,
    val stockService: StockService,
    val transferOrderService: TransferOrderService,
    val transferOrderProductService: TransferOrderProductService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ReceivingOrderProductConversions
       with FindAllFeature {

  type Dao = ReceivingOrderProductDao
  type Entity = ReceivingOrderProductDetails
  type Expansions = ReceivingOrderProductExpansions
  type Filters = ReceivingOrderProductFilters
  type Record = ReceivingOrderProductRecord

  protected val dao = daos.receivingOrderProductDao

  val articleValidator = new ArticleValidator

  def countProductsByReceivingOrderIds(
      receivingOrders: Seq[ReceivingOrderRecord],
    ): Future[Map[ReceivingOrderRecord, BigDecimal]] = {
    val receivingOrderIds = receivingOrders.map(_.id)
    dao.countProductsByReceivingOrderIds(receivingOrderIds).map(_.mapKeysToRecords(receivingOrders))
  }

  def findStockValueByReceivingOrderIds(
      receivingOrders: Seq[ReceivingOrderRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[ReceivingOrderRecord, MonetaryAmount]] = {
    val receivingOrderIds = receivingOrders.map(_.id)
    dao
      .findStockValueByReceivingOrderIds(receivingOrderIds)
      .map(_.transform((_, v) => MonetaryAmount(v)).mapKeysToRecords(receivingOrders))
  }

  def findByPurchaseOrderIdsAndProductIds(
      purchaseOrderIds: Seq[UUID],
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[ReceivingOrderProduct]]] =
    dao
      .findByPurchaseOrderIdsAndProductIds(purchaseOrderIds, productIds)
      .map(_.transform((_, v) => v.map(fromRecordToEntity)))

  def convertToReceivingOrderProductUpdates(
      receivingOrderId: UUID,
      update: ReceivingOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ReceivingOrderProductUpdate]]]] =
    update.products match {
      case Some(receivingOrderProducts) =>
        val productIds = receivingOrderProducts.map(_.productId)
        articleValidator.accessByIds(productIds).mapNested { products =>
          val productsMap = products.groupBy(_.id).transform((_, v) => v.head)
          Some(fromUpsertionToUpdates(receivingOrderId, receivingOrderProducts, productsMap))
        }
      case None => Future.successful(Multiple.empty)
    }

  def findByReceivingOrderId(id: UUID): Future[Seq[Record]] = dao.findByReceivingOrderId(id)

  def enrich(
      records: Seq[Record],
      f: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    getReceivingOrder(f.receivingOrderId).flatMap {
      case Some(receivingOrder) =>
        for {
          products <- getProducts(records)(expansions)
          purchaseOrderProductsPerProduct <- getPurchaseOrderProducts(receivingOrder.purchaseOrder)
          transferOrderProductsPerProduct <- getTransferOrderProducts(receivingOrder.transferOrder)
          stockLevelPerProduct <- getStockLevelsPerProduct(receivingOrder, products.keys.toSeq)
        } yield toReceivingOrderProductDetails(
          records,
          products,
          purchaseOrderProductsPerProduct,
          transferOrderProductsPerProduct,
          stockLevelPerProduct,
        )
      case _ => Future.successful(Seq.empty)
    }

  private def getProducts(
      records: Seq[Record],
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Product]] = {
    val productIds = records.map(_.productId)
    articleService.getByIds(productIds)(e.toArticleExpansions).map(_.map(p => p.id -> p).toMap)
  }

  private def getReceivingOrder(receivingOrderId: UUID)(implicit user: UserContext): Future[Option[ReceivingOrder]] =
    receivingOrderService.findById(receivingOrderId)(ReceivingOrderExpansions.withPurchaseOrTransferOrderAndLocation)

  private def getPurchaseOrderProducts(
      maybePurchaseOrder: Option[PurchaseOrder],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[PurchaseOrderProduct]]] =
    maybePurchaseOrder match {
      case Some(purchaseOrder) =>
        val entities = purchaseOrderProductService.findByPurchaseOrderId(purchaseOrder.id)
        entities.map(_.groupBy(_.productId))
      case None => Future.successful(Map.empty)
    }

  private def getTransferOrderProducts(
      maybeTransferOrder: Option[TransferOrder],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[TransferOrderProduct]]] =
    maybeTransferOrder match {
      case Some(transferOrder) =>
        val entities = transferOrderProductService.findByTransferOrderId(transferOrder.id)
        entities.map(_.groupBy(_.productId))
      case None => Future.successful(Map.empty)
    }

  private def getStockLevelsPerProduct(
      receivingOrder: ReceivingOrder,
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, BigDecimal]] = {
    val locationId =
      receivingOrder.locationId

    stockService
      .findStockLevelByVariantArticleIds(productIds, Seq(locationId).some)
      .map(_.transform((_, v) => v.getOrElse(locationId, 0)))
  }

  def syncProductsByReceivingOrder(receivingOrder: ReceivingOrderRecord)(implicit user: UserContext): Future[Unit] = {
    val toLocationId = receivingOrder.locationId
    for {
      records <- findByReceivingOrderId(receivingOrder.id)
      fromLocationId <- getFromLocationId(receivingOrder)
      productIds = records.map(_.productId)
      locationIds = fromLocationId.toSeq ++ Seq(toLocationId)
      stockRecords <- stockService.findAllPerProductIdsAndLocationIds(productIds, locationIds)
      createdStocks <- createMissingStocks(toLocationId, records, stockRecords)
      _ <- stockService.bulkUpdate(toStockUpdates(records, fromLocationId, toLocationId, createdStocks ++ stockRecords))
    } yield ()
  }

  private def getFromLocationId(
      receivingOrder: ReceivingOrderRecord,
    )(implicit
      user: UserContext,
    ): Future[Option[UUID]] =
    (receivingOrder.receivingObjectType, receivingOrder.receivingObjectId) match {
      case (Some(ReceivingOrderObjectType.Transfer), Some(id)) =>
        val entity = transferOrderService.findById(id)(TransferOrderExpansions(withFromLocation = true))
        entity.map(_.flatMap(_.fromLocation.map(_.id)))
      case _ => Future.successful(None)
    }

  private def toStockUpdates(
      records: Seq[Record],
      fromLocationId: Option[UUID],
      toLocationId: UUID,
      stocksPerProductIdAndLocationId: Map[(UUID, UUID), StockRecord],
    ): Seq[StockUpdate] = {

    def toLocationStockUpdate(
        record: Record,
        locationId: UUID,
        reason: QuantityChangeReason,
      ): Option[StockUpdate] =
      stocksPerProductIdAndLocationId.get((record.productId, locationId)).map { existingStock =>
        val quantityIncrease = record.quantity.getOrElse[BigDecimal](0)
        val op = { (a: BigDecimal, b: BigDecimal) =>
          if (reason == QuantityChangeReason.Receiving) a + b
          else a - b
        }
        val updatedQuantity = op(existingStock.quantity, quantityIncrease)
        toStockUpdate(locationId, record, updatedQuantity, reason)
      }

    records.flatMap { record =>
      val receivingStock = toLocationStockUpdate(record, toLocationId, QuantityChangeReason.Receiving)
      val leavingStock =
        fromLocationId.flatMap(lId => toLocationStockUpdate(record, lId, QuantityChangeReason.Transfer))
      receivingStock.toSeq ++ leavingStock.toSeq
    }
  }

  private def toStockUpdate(
      locationId: UUID,
      record: Record,
      quantity: BigDecimal,
      reason: QuantityChangeReason,
    ): StockUpdate = {
    val notes = if (reason == QuantityChangeReason.Receiving) "Receiving Order" else "Transfer Order"
    StockUpdate(
      locationId = locationId,
      productId = record.productId,
      quantity = Some(quantity),
      minimumOnHand = None,
      reorderAmount = None,
      sellOutOfStock = None,
      reason = reason,
      notes = Some(notes),
    )
  }

  private def createMissingStocks(
      locationId: UUID,
      records: Seq[Record],
      existingStocks: Map[(UUID, UUID), StockRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[(UUID, UUID), StockRecord]] = {
    val recordsWithNoStocks = records.filterNot(r => existingStocks.get((r.productId, locationId)).isDefined)
    stockService.createEmptyStocks(recordsWithNoStocks.map(r => r.productId -> locationId).toMap)
  }

  def findAverageCostByProductIds(productIds: Seq[UUID]): Future[Map[UUID, BigDecimal]] =
    dao.findAverageCostByProductIdsAndLocationId(productIds, None)

  def findAverageCostByProductIdsAndLocationId(productIds: Seq[UUID], locationId: UUID): Future[Map[UUID, BigDecimal]] =
    dao.findAverageCostByProductIdsAndLocationId(productIds, Some(locationId))
}
