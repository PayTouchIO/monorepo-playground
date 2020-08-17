package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType
import io.paytouch.core.entities.enums.ExposedName

final case class ProductRevenue(productId: UUID, revenuePerLocation: Seq[ProductRevenuePerLocation])
    extends ExposedEntity {
  val classShortName = ExposedName.ProductRevenue
}

final case class ProductRevenuePerLocation(
    locationId: UUID,
    avgPrice: MonetaryAmount,
    avgDiscount: MonetaryAmount,
    avgCost: MonetaryAmount,
    avgMargin: BigDecimal,
    totalSold: ProductQuantity,
    totalRevenue: MonetaryAmount,
    totalTax: MonetaryAmount,
    totalProfit: MonetaryAmount,
  )

final case class ProductQuantity(amount: BigDecimal, unit: UnitType)
object ProductQuantity {
  val Zero: ProductQuantity =
    ProductQuantity(0, UnitType.`Unit`)
}
