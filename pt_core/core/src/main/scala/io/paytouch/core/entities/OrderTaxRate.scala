package io.paytouch.core.entities

import java.util.UUID

final case class OrderTaxRate(
    id: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
  )

final case class OrderTaxRateUpsertion(
    id: Option[UUID],
    taxRateId: UUID,
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
  )

final case class OrderItemTaxRate(
    id: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: Option[BigDecimal],
    applyToPrice: Boolean,
    active: Boolean,
  )

final case class OrderItemTaxRateUpsertion(
    id: Option[UUID],
    taxRateId: Option[UUID], // Temp HOT-FIX for bug register PR-1488
    name: String,
    value: BigDecimal,
    totalAmount: Option[BigDecimal],
    applyToPrice: Boolean,
    active: Boolean = true,
  )
