package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.calculations.ProductStocksCalculations
import io.paytouch.core.conversions.TransferOrderProductConversions
import io.paytouch.core.data.daos.{ Daos, TransferOrderProductDao }
import io.paytouch.core.data.model.{ TransferOrderProductRecord, TransferOrderProductUpdate, TransferOrderRecord }
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ TransferOrderExpansions, TransferOrderProductExpansions }
import io.paytouch.core.filters.TransferOrderProductFilters
import io.paytouch.core.RichMap
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ArticleValidator

import scala.concurrent._

class TransferOrderProductService(
    val articleService: ArticleService,
    val productLocationService: ProductLocationService,
    val stockService: StockService,
    transferOrderService: => TransferOrderService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TransferOrderProductConversions
       with ProductStocksCalculations
       with FindAllFeature {

  type Dao = TransferOrderProductDao
  type Entity = TransferOrderProduct
  type Expansions = TransferOrderProductExpansions
  type Filters = TransferOrderProductFilters
  type Record = TransferOrderProductRecord

  protected val dao = daos.transferOrderProductDao

  val articleValidator = new ArticleValidator

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val transferOrderR = transferOrderService.findById(filters.transferOrderId, transferOrderService.defaultFilters)(
      TransferOrderExpansions(withFromLocation = true, withToLocation = true),
    )
    val productPerTransferOrderProductR = getProductPerTransferOrderProduct(records)(expansions)
    for {
      transferOrder <- transferOrderR
      productPerTransferOrderProduct <- productPerTransferOrderProductR
      fromLocationId = transferOrder.flatMap(_.fromLocation.map(_.id))
      toLocationId = transferOrder.flatMap(_.toLocation.map(_.id))
      fromCurrentQuantityPerTransferOrderProduct <- getCurrentQuantityPerTransferOrderProduct(records, fromLocationId)
      toCurrentQuantityPerTransferOrderProduct <- getCurrentQuantityPerTransferOrderProduct(records, toLocationId)
      totalValuePerTransferOrderProduct <- getTotalValuePerTransferOrderProduct(records, toLocationId)
    } yield fromRecordsAndExpansionsToEntities(
      records,
      productPerTransferOrderProduct,
      fromCurrentQuantityPerTransferOrderProduct,
      toCurrentQuantityPerTransferOrderProduct,
      totalValuePerTransferOrderProduct,
    )
  }

  private def getProductPerTransferOrderProduct(
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

  def getCurrentQuantityPerTransferOrderProduct(
      transferOrderProducts: Seq[Record],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[TransferOrderProductRecord, BigDecimal]] =
    locationId match {
      case Some(lId) =>
        val productIds = transferOrderProducts.map(_.productId)
        stockService.findAllPerProductIdsAndLocationId(productIds, lId).map { stockLevels =>
          transferOrderProducts.map { transferOrderProduct =>
            val stock = stockLevels.get((transferOrderProduct.productId, lId))
            transferOrderProduct -> stock.map(_.quantity).getOrElse[BigDecimal](0)
          }.toMap
        }
      case _ => Future.successful(Map.empty[TransferOrderProductRecord, BigDecimal])
    }

  def getTotalValuePerTransferOrderProduct(
      transferOrderProducts: Seq[Record],
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[TransferOrderProductRecord, MonetaryAmount]] =
    locationId match {
      case Some(lId) =>
        val pIds = transferOrderProducts.map(_.productId)
        productLocationService.findAllByProductIdsAndLocationIds(productIds = pIds, locationIds = Seq(lId)).map {
          productLocations =>
            transferOrderProducts.map { transferOrderProduct =>
              val totalValue = computeStockValue(
                locationId = lId,
                transferOrderProduct = transferOrderProduct,
                productLocations = productLocations,
              )
              transferOrderProduct -> totalValue
            }.toMap
        }
      case _ => Future.successful(Map.empty[TransferOrderProductRecord, MonetaryAmount])
    }

  def countProductsByTransferOrderIds(
      transferOrders: Seq[TransferOrderRecord],
    ): Future[Map[TransferOrderRecord, BigDecimal]] = {
    val transferOrderIds = transferOrders.map(_.id)
    dao.countProductsByTransferOrderIds(transferOrderIds).map(_.mapKeysToRecords(transferOrders))
  }

  def findStockValueByTransferOrderIds(
      transferOrders: Seq[TransferOrderRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[TransferOrderRecord, MonetaryAmount]] = {
    val transferOrderIds = transferOrders.map(_.id)
    val locationIds = transferOrders.map(_.toLocationId) ++ transferOrders.map(_.fromLocationId)
    for {
      transferOrderProducts <- dao.findByTransferOrderIds(transferOrderIds)
      productIds = transferOrderProducts.map(_.productId)
      productLocations <- productLocationService.findAllByProductIdsAndLocationIds(productIds, locationIds)
    } yield {
      val transferOrderProductsPerTransferOrder =
        transferOrderProducts.groupBy(_.transferOrderId).mapKeysToRecords(transferOrders)
      transferOrderProductsPerTransferOrder.map {
        case (transferOrder, tranOrdProds) =>
          transferOrder -> computeStockValue(transferOrder, tranOrdProds, productLocations)
      }
    }
  }

  def convertToTransferOrderProductUpdates(
      transferOrderId: UUID,
      update: TransferOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[TransferOrderProductUpdate]]]] =
    update.products match {
      case Some(transferOrderProducts) =>
        val productIds = transferOrderProducts.map(_.productId)
        articleValidator.accessByIds(productIds).mapNested { products =>
          val productsMap = products.groupBy(_.id).transform((_, v) => v.head)
          Some(fromUpsertionToUpdates(transferOrderId, transferOrderProducts, productsMap))
        }

      case None =>
        Future.successful(Multiple.empty)
    }

  def findByTransferOrderId(
      transferOrderId: UUID,
    )(implicit
      userContext: UserContext,
    ): Future[Seq[TransferOrderProduct]] =
    for {
      records <- dao.findByTransferOrderId(transferOrderId)
      entities <- enrich(records, TransferOrderProductFilters(transferOrderId))(TransferOrderProductExpansions.empty)
    } yield entities
}
