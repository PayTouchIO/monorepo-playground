package io.paytouch.core.conversions

import io.paytouch.core.calculations.Calculations
import io.paytouch.core.data.model._
import io.paytouch.core.entities._

trait InventoryConversions extends Calculations {

  def fromRecordsToEntities(
      mainProducts: Seq[ArticleRecord],
      allStorableProducts: Seq[ArticleRecord],
      productLocationsPerProduct: Map[ArticleRecord, Seq[ProductLocationRecord]],
      stocksPerProduct: Map[ArticleRecord, Seq[StockRecord]],
      variantsPerProduct: Map[ArticleRecord, Seq[VariantOptionWithType]],
    )(implicit
      user: UserContext,
    ): Seq[Inventory] =
    mainProducts.map { mainProduct =>
      val storableProducts = allStorableProducts.filter(_.isVariantOfProductId.contains(mainProduct.id))
      val allProducts = Seq(mainProduct) ++ storableProducts
      val productLocations = productLocationsPerProduct.view.filterKeys(allProducts.contains).values.flatten.toSeq
      val stocks = stocksPerProduct.view.filterKeys(allProducts.contains).values.flatten.toSeq
      val options = variantsPerProduct.getOrElse(mainProduct, Seq.empty)
      fromRecordsToEntity(mainProduct, productLocations, stocks, options)
    }

  def fromRecordsToEntity(
      mainProduct: ArticleRecord,
      productLocations: Seq[ProductLocationRecord],
      stocks: Seq[StockRecord],
      options: Seq[VariantOptionWithType],
    )(implicit
      user: UserContext,
    ): Inventory =
    Inventory(
      id = mainProduct.id,
      name = mainProduct.name,
      sku = mainProduct.sku,
      upc = mainProduct.upc,
      isVariantOfProductId = mainProduct.isVariantOfProductId,
      options = options,
      totalQuantity = computeTotalQuantity(mainProduct, stocks),
      stockValue = computeStockValue(productLocations, stocks),
    )

}
