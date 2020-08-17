package io.paytouch.ordering.entities

import java.util.UUID

final case class CartItemTaxRate(
    id: UUID,
    taxRateId: UUID,
    name: String,
    value: BigDecimal,
    total: MonetaryAmount,
    applyToPrice: Boolean,
  )

final case class CartItemTaxRateUpsertion(
    id: UUID,
    taxRateId: UUID,
    name: Option[String],
    value: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
    applyToPrice: Option[Boolean],
  )
