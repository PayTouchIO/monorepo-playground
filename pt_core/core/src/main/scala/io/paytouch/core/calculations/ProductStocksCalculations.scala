package io.paytouch.core.calculations

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ MonetaryAmount, ProductQuantity, UserContext }

trait ProductStocksCalculations {
  import CalculationUtils._

  def computeTotalQuantity(product: ArticleRecord, stocks: Seq[StockRecord]): ProductQuantity = {
    val unit = product.unit
    val amount = stocks.map(_.quantity).sum
    ProductQuantity(amount, unit)
  }

  def computeStockValue(
      productLocations: Seq[ProductLocationRecord],
      stocks: Seq[StockRecord],
    )(implicit
      user: UserContext,
    ): MonetaryAmount = {
    val nonZeroCostProductLocations =
      productLocations.filterNot(pl => pl.costAmount.isEmpty || pl.costAmount.contains(BigDecimal(0)))
    val amounts = stocks.flatMap { stock =>
      nonZeroCostProductLocations
        .filter(_.contains(stock.productId, stock.locationId))
        .map(productLocation => computeStockValue(productLocation, stock.quantity))
    }
    amounts.sumNonZero
  }

  def computeStockValue(
      productLocation: ProductLocationRecord,
      quantity: BigDecimal,
    )(implicit
      user: UserContext,
    ): MonetaryAmount = {
    val cost = productLocation.costAmount.getOrElse[BigDecimal](0)
    MonetaryAmount(cost * quantity, user.currency)
  }

  def computeStockValue(
      transferOrder: TransferOrderRecord,
      transferOrderProducts: Seq[TransferOrderProductRecord],
      productLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserContext,
    ): MonetaryAmount =
    transferOrderProducts.map { transferOrderProduct =>
      val productId = transferOrderProduct.productId
      val locationId = transferOrder.toLocationId
      val quantity = transferOrderProduct.quantity.getOrElse(BigDecimal(0))
      computeStockValue(productId, locationId, quantity, productLocations)
    }.sumNonZero

  def computeStockValue(
      locationId: UUID,
      transferOrderProduct: TransferOrderProductRecord,
      productLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserContext,
    ): MonetaryAmount = {
    val productId = transferOrderProduct.productId
    val quantity = transferOrderProduct.quantity.getOrElse(BigDecimal(0))
    computeStockValue(productId, locationId, quantity, productLocations)
  }

  def computeStockValue(
      returnOrder: ReturnOrderRecord,
      returnOrderProducts: Seq[ReturnOrderProductRecord],
      productLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserContext,
    ): MonetaryAmount =
    returnOrderProducts.map { returnOrderProduct =>
      val productId = returnOrderProduct.productId
      val locationId = returnOrder.locationId
      val quantity = returnOrderProduct.quantity.getOrElse(BigDecimal(0))
      computeStockValue(productId, locationId, quantity, productLocations)
    }.sumNonZero

  def computeStockValue(
      productId: UUID,
      locationId: UUID,
      quantity: BigDecimal,
      productLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserContext,
    ): MonetaryAmount = {
    val validProductLocations = productLocations.filter(_.contains(productId, locationId))
    validProductLocations.map(pl => computeStockValue(pl, quantity)).sumNonZero
  }

}
