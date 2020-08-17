package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.RichMap
import io.paytouch.core.conversions.InventoryCountProductConversions
import io.paytouch.core.data.daos.{ Daos, InventoryCountProductDao }
import io.paytouch.core.data.model.enums.{ InventoryCountStatus, QuantityChangeReason }
import io.paytouch.core.data.model.{
  InventoryCountProductRecord,
  InventoryCountProductUpdate,
  InventoryCountRecord,
  StockRecord,
}
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ ArticleExpansions, InventoryCountProductExpansions }
import io.paytouch.core.filters.InventoryCountProductFilters
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ArticleValidator

import scala.concurrent._

class InventoryCountProductService(
    val articleService: ArticleService,
    val stockService: StockService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends InventoryCountProductConversions
       with FindAllFeature {

  type Dao = InventoryCountProductDao
  type Entity = InventoryCountProduct
  type Expansions = InventoryCountProductExpansions
  type Filters = InventoryCountProductFilters
  type Record = InventoryCountProductRecord
  type Validator = ArticleValidator

  protected val dao = daos.inventoryCountProductDao

  val articleValidator = new ArticleValidator

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    for {
      productPerInventoryCountProduct <- getProductPerInventoryCountProduct(records)(expansions)
    } yield fromRecordsAndExpansionsToEntities(records, productPerInventoryCountProduct)

  private def getProductPerInventoryCountProduct(
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

  def convertToInventoryCountProductUpdates(
      inventoryCountId: UUID,
      update: InventoryCountUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[InventoryCountProductUpdate]]]] =
    update.products match {
      case Some(inventoryCountProducts) =>
        val productIds = inventoryCountProducts.map(_.productId)
        articleValidator.accessByIds(productIds).flatMapTraverse { productRecords =>
          articleService.enrich(productRecords, articleService.defaultFilters)(ArticleExpansions.empty).map {
            products =>
              val productsMap = products.groupBy(_.id).transform((_, v) => v.head)
              Some(fromUpsertionToUpdates(inventoryCountId, update.locationId, inventoryCountProducts, productsMap))
          }
        }
      case None => Future.successful(Multiple.empty)
    }

  def countProductsByInventoryCountIds(
      inventoryCounts: Seq[InventoryCountRecord],
    ): Future[Map[InventoryCountRecord, Int]] = {
    val inventoryCountIds = inventoryCounts.map(_.id)
    dao.countOrderedProductsByInventoryCountIds(inventoryCountIds).map(_.mapKeysToRecords(inventoryCounts))
  }

  def syncProductsByInventoryCount(
      inventoryCount: InventoryCountRecord,
    )(implicit
      user: UserContext,
    ): Future[InventoryCountStatus] = {
    val locationId = inventoryCount.locationId

    def toStockUpdates(records: Seq[Record]): Seq[StockUpdate] =
      records.map(toStockUpdate)

    def toStockUpdate(record: Record): StockUpdate =
      StockUpdate(
        locationId = locationId,
        productId = record.productId,
        quantity = record.countedQuantity,
        minimumOnHand = None,
        reorderAmount = None,
        sellOutOfStock = None,
        reason = QuantityChangeReason.Manual,
        notes = Some("Inventory Count"),
      )

    def inferStatus(records: Seq[InventoryCountProductRecord]): InventoryCountStatus =
      if (records.forall(r => r.countedQuantity == r.expectedQuantity)) InventoryCountStatus.Matched
      else InventoryCountStatus.Unmatched

    for {
      records <- dao.findByInventoryCountId(inventoryCount.id)
      _ <- stockService.bulkUpdate(toStockUpdates(records))
    } yield inferStatus(records)

  }

}
