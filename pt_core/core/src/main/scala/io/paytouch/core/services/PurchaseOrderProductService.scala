package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.conversions.PurchaseOrderProductConversions
import io.paytouch.core.data.daos.{ Daos, PurchaseOrderProductDao }
import io.paytouch.core.data.model.{ PurchaseOrderProductRecord, PurchaseOrderProductUpdate, PurchaseOrderRecord }
import io.paytouch.core.entities._
import io.paytouch.core.expansions.PurchaseOrderProductExpansions
import io.paytouch.core.filters.PurchaseOrderProductFilters
import io.paytouch.core.RichMap
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ArticleValidator

class PurchaseOrderProductService(
    val articleService: ArticleService,
    receivingOrderProductService: => ReceivingOrderProductService,
    val stockService: StockService,
    val returnOrderProductService: ReturnOrderProductService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends PurchaseOrderProductConversions
       with FindAllFeature {

  type Dao = PurchaseOrderProductDao
  type Entity = PurchaseOrderProduct
  type Expansions = PurchaseOrderProductExpansions
  type Filters = PurchaseOrderProductFilters
  type Record = PurchaseOrderProductRecord
  type Validator = ArticleValidator

  protected val dao = daos.purchaseOrderProductDao
  val purchaseOrderDao = daos.purchaseOrderDao

  val articleValidator = new ArticleValidator

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val productPerPurchaseOrderProductR = getProductPerPurchaseOrderProduct(records)(expansions)
    val receivingOrderProductsPerPurchaseOrderProductR = getReceivingOrderProductsPerPurchaseOrderProduct(records)
    val quantityReturnedPerPurchaseOrderProductR = getQuantityReturnedPerPurchaseOrderProduct(records)
    for {
      productPerPurchaseOrderProduct <- productPerPurchaseOrderProductR
      receivingOrderProductsPerPurchaseOrderProduct <- receivingOrderProductsPerPurchaseOrderProductR
      quantityReturnedPerPurchaseOrderProduct <- quantityReturnedPerPurchaseOrderProductR
      stockPerPurchaseOrderProduct <- getStockLevelPerPurchaseOrderProduct(records, filters.purchaseOrderId)
    } yield fromRecordsAndExpansionsToEntities(
      records,
      productPerPurchaseOrderProduct,
      receivingOrderProductsPerPurchaseOrderProduct,
      quantityReturnedPerPurchaseOrderProduct,
      stockPerPurchaseOrderProduct,
    )
  }

  private def getProductPerPurchaseOrderProduct(
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

  private def getReceivingOrderProductsPerPurchaseOrderProduct(
      items: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Map[Record, Seq[ReceivingOrderProduct]]] =
    receivingOrderProductService
      .findByPurchaseOrderIdsAndProductIds(items.map(_.purchaseOrderId).distinct, items.map(_.productId))
      .map(_.flatMap {
        case (k, v) =>
          items.find(_.productId == k).map(item => (item, v))
      })

  private def getQuantityReturnedPerPurchaseOrderProduct(
      items: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Map[Record, BigDecimal]] =
    returnOrderProductService
      .findByPurchaseOrderIdsAndProductIds(items.map(_.purchaseOrderId).distinct, items.map(_.productId))
      .map(_.flatMap {
        case (k, v) =>
          items.find(_.productId == k).map(item => (item, v.map(_.quantity.getOrElse[BigDecimal](0)).sum))
      })

  private def getStockLevelPerPurchaseOrderProduct(
      items: Seq[Record],
      purchaseOrderId: UUID,
    )(implicit
      user: UserContext,
    ): Future[Map[Record, BigDecimal]] =
    purchaseOrderDao.findById(purchaseOrderId).flatMap {
      case Some(purchaseOrder) =>
        val productId = items.map(_.productId)
        val locationId = purchaseOrder.locationId
        stockService.findStockLevelByVariantArticleIds(productId, Seq(locationId).some).map { result =>
          result.flatMap {
            case (pId, stockLevel) =>
              items.find(_.productId == pId).map(item => (item, stockLevel.getOrElse[BigDecimal](locationId, 0)))
          }
        }
      case _ => Future.successful(Map.empty)
    }

  def countOrderedProductsByPurchaseOrderIds(
      purchaseOrders: Seq[PurchaseOrderRecord],
    ): Future[Map[PurchaseOrderRecord, BigDecimal]] = {
    val purchaseOrderIds = purchaseOrders.map(_.id)
    dao.countOrderedProductsByPurchaseOrderIds(purchaseOrderIds).map(_.mapKeysToRecords(purchaseOrders))
  }

  def convertToPurchaseOrderProductUpdates(
      purchaseOrderId: UUID,
      update: PurchaseOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[PurchaseOrderProductUpdate]]]] =
    update.products match {
      case Some(purchaseOrderProducts) =>
        val productIds = purchaseOrderProducts.map(_.productId)
        articleValidator.accessByIds(productIds).mapNested { _ =>
          Some(fromUpsertionToUpdates(purchaseOrderId, purchaseOrderProducts))
        }
      case None => Future.successful(Multiple.empty)
    }

  def findByPurchaseOrderId(
      purchaseOrderId: UUID,
    )(implicit
      userContext: UserContext,
    ): Future[Seq[PurchaseOrderProduct]] =
    for {
      records <- dao.findByPurchaseOrderId(purchaseOrderId)
      entities <- enrich(records, PurchaseOrderProductFilters(purchaseOrderId))(PurchaseOrderProductExpansions.empty)
    } yield entities

}
