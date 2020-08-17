package io.paytouch.core.services

import io.paytouch.core.conversions.InventoryConversions
import io.paytouch.core.data.daos.{ Daos, InventoryDao }
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.{ InventoryFilters, ProductRevenueFilters }
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.validators.ArticleValidator

import scala.concurrent._

class InventoryService(
    val orderItemService: OrderItemService,
    val productLocationService: ProductLocationService,
    val stockService: StockService,
    val variantService: VariantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends InventoryConversions
       with FindAllFeature {

  type Dao = InventoryDao
  type Entity = Inventory
  type Expansions = NoExpansions
  type Filters = InventoryFilters
  type Record = ArticleRecord
  type Validator = ArticleValidator

  protected val dao = daos.inventoryDao
  protected val validator = new ArticleValidator

  def enrich(
      mainProducts: Seq[Record],
      f: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    for {
      storableProducts <- dao.findStorableProductsByMainProductIds(mainProducts.map(_.id))
      allProducts = mainProducts ++ storableProducts
      productLocationsPerProduct <- productLocationService.findAllPerProduct(allProducts, f.locationIds)
      stocksPerProduct <- stockService.findAllPerProduct(allProducts, f.locationIds)
      variantOptions <-
        variantService
          .findVariantOptionsByVariantIds(storableProducts.map(_.id))
          .map(_.mapKeysToRecords(storableProducts))
    } yield fromRecordsToEntities(
      mainProducts,
      storableProducts,
      productLocationsPerProduct,
      stocksPerProduct,
      variantOptions,
    )

  def computeRevenue(filters: ProductRevenueFilters)(implicit user: UserContext): Future[Option[ProductRevenue]] =
    orderItemService.computeRevenue(filters).map(_.toOption)

}
