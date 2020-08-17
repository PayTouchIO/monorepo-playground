package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class TaxRate(
    id: UUID,
    name: String,
    value: BigDecimal,
    applyToPrice: Boolean,
    locationOverrides: Map[UUID, ItemLocation],
  )
