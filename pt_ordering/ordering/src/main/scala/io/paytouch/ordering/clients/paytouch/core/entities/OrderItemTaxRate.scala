package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

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
    id: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: Option[BigDecimal],
    applyToPrice: Boolean,
    active: Boolean = true,
  )
