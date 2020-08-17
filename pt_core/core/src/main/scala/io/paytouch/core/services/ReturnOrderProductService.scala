package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.calculations.ProductStocksCalculations
import io.paytouch.core.conversions.ReturnOrderProductConversions
import io.paytouch.core.data.daos.{ Daos, ReturnOrderProductDao }
import io.paytouch.core.data.model.{ ReturnOrderUpdate => _, StockUpdate => _, _ }
import io.paytouch.core.data.model.enums.QuantityChangeReason
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ ReturnOrderExpansions, ReturnOrderProductExpansions }
import io.paytouch.core.filters.ReturnOrderProductFilters
import io.paytouch.core.RichMap
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ArticleValidator

class ReturnOrderProductService(
    val productLocationService: ProductLocationService,
    val articleService: ArticleService,
    returnOrderService: => ReturnOrderService,
    stockService: StockService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ProductStocksCalculations
       with ReturnOrderProductConversions
       with FindAllFeature {

  type Dao = ReturnOrderProductDao
  type Entity = ReturnOrderProduct
  type Expansions = ReturnOrderProductExpansions
  type Filters = ReturnOrderProductFilters
  type Record = ReturnOrderProductRecord

  protected val dao = daos.returnOrderProductDao

  val articleValidator = new ArticleValidator

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    getReturnOrder(filters.returnOrderId).flatMap {
      case Some(returnOrder) =>
        for {
          productPerReturnOrderProduct <- getProductPerReturnOrderProduct(records)(expansions)
          stockPerReturnOrderProduct <- getStocksByProductIdsAndLocationId(records, returnOrder)
        } yield fromRecordsAndExpansionsToEntities(records, productPerReturnOrderProduct, stockPerReturnOrderProduct)
      case _ => Future.successful(Seq.empty)
    }

  private def getReturnOrder(returnOrderId: UUID)(implicit user: UserContext): Future[Option[ReturnOrder]] =
    returnOrderService.findById(returnOrderId)(ReturnOrderExpansions(withLocation = true))

  private def getProductPerReturnOrderProduct(
      records: Seq[Record],
    )(
      e: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Map[Record, Product]] =
    getRelatedField[Product](
      articleService.getByIds(_)(e.toArticleExpansions),
      _.id,
      _.productId,
      records,
    )

  def getStocksByProductIdsAndLocationId(
      items: Seq[Record],
      returnOrder: ReturnOrder,
    )(implicit
      user: UserContext,
    ): Future[Map[Record, BigDecimal]] = {
    val locationId =
      returnOrder.locationId

    stockService
      .findStockLevelByVariantArticleIds(items.map(_.productId), Seq(locationId).some)
      .map { result =>
        result.flatMap {
          case (productId, stockLevel) =>
            items.find(_.productId == productId).map(item => (item, stockLevel.getOrElse[BigDecimal](locationId, 0)))
        }
      }
  }

  def findStockValueByReturnOrders(
      returnOrders: Seq[ReturnOrderRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[ReturnOrderRecord, MonetaryAmount]] = {
    val returnOrderIds = returnOrders.map(_.id)
    val locationIds = returnOrders.map(_.locationId)
    for {
      returnOrderProducts <- dao.findByReturnOrderIds(returnOrderIds)
      productIds = returnOrderProducts.map(_.productId)
      productLocations <- productLocationService.findAllByProductIdsAndLocationIds(productIds, locationIds)
    } yield {
      val returnOrderProductsPerReturnOrder =
        returnOrderProducts.groupBy(_.returnOrderId).mapKeysToRecords(returnOrders)
      returnOrderProductsPerReturnOrder.map {
        case (returnOrder, retOrdPrds) =>
          returnOrder -> computeStockValue(returnOrder, retOrdPrds, productLocations)
      }
    }
  }

  def countProductsByReturnOrderIds(
      returnOrders: Seq[ReturnOrderRecord],
    ): Future[Map[ReturnOrderRecord, BigDecimal]] = {
    val returnOrderIds = returnOrders.map(_.id)
    dao.countProductsByReturnOrderIds(returnOrderIds).map(_.mapKeysToRecords(returnOrders))
  }

  def convertToReturnOrderProductUpdates(
      returnOrderId: UUID,
      update: ReturnOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ReturnOrderProductUpdate]]]] =
    update.products match {
      case Some(returnOrderProducts) =>
        val productIds = returnOrderProducts.map(_.productId)

        articleValidator.accessByIds(productIds).mapNested { products =>
          val productsMap = products.groupBy(_.id).transform((_, v) => v.head)
          Some(fromUpsertionToUpdates(returnOrderId, returnOrderProducts, productsMap))
        }

      case None =>
        Future.successful(Multiple.empty)
    }

  def syncProductsByReceivingOrder(returnOrder: ReturnOrderRecord)(implicit user: UserContext): Future[Unit] = {
    val toLocationId = returnOrder.locationId
    for {
      records <- dao.findByReturnOrderId(returnOrder.id)
      productIds = records.map(_.productId)
      stockRecords <- stockService.findAllPerProductIdsAndLocationIds(productIds, Seq(returnOrder.locationId))
      _ <- stockService.bulkUpdate(toStockUpdates(records, toLocationId, stockRecords))
    } yield ()
  }

  def findByPurchaseOrderIdsAndProductIds(
      purchaseOrderIds: Seq[UUID],
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[ReturnOrderProductRecord]]] =
    dao.findByPurchaseOrderIdsAndProductIds(purchaseOrderIds, productIds)

  private def toStockUpdates(
      records: Seq[Record],
      toLocationId: UUID,
      stocksPerProductIdAndLocationId: Map[(UUID, UUID), StockRecord],
    ): Seq[StockUpdate] = {

    def toLocationStockUpdate(
        record: Record,
        locationId: UUID,
        reason: QuantityChangeReason,
      ): Option[StockUpdate] =
      stocksPerProductIdAndLocationId.get((record.productId, locationId)).map { existingStock =>
        val quantityDecrease = record.quantity.getOrElse[BigDecimal](0)
        val updatedQuantity = existingStock.quantity - quantityDecrease
        toStockUpdate(locationId, record, updatedQuantity, reason)
      }

    records.flatMap(record => toLocationStockUpdate(record, toLocationId, QuantityChangeReason.SupplierReturn))
  }

  private def toStockUpdate(
      locationId: UUID,
      record: Record,
      quantity: BigDecimal,
      reason: QuantityChangeReason,
    ): StockUpdate =
    StockUpdate(
      locationId = locationId,
      productId = record.productId,
      quantity = Some(quantity),
      minimumOnHand = None,
      reorderAmount = None,
      sellOutOfStock = None,
      reason = reason,
      notes = Some("Return Order"),
    )
}
