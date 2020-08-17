package io.paytouch.core.reports.entities

import java.util.UUID

import io.paytouch.core.entities.{ MonetaryAmount, VariantOptionWithType }

final case class ProductTop(
    id: UUID,
    name: String,
    quantitySold: BigDecimal,
    netSales: MonetaryAmount,
    revenue: MonetaryAmount,
    profit: MonetaryAmount,
    margin: BigDecimal,
    options: Option[Seq[VariantOptionWithType]],
  )
