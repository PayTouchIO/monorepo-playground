package io.paytouch.ordering.entities

import java.util.UUID

final case class CartTaxRate(
    id: UUID,
    taxRateId: UUID,
    name: String,
    `value`: BigDecimal,
    total: MonetaryAmount,
  )
