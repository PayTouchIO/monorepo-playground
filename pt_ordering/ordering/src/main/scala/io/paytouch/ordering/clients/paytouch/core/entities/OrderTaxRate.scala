package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID
import io.paytouch.ordering.entities.MonetaryAmount

final case class OrderTaxRate(
    id: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
  )

final case class OrderTaxRateUpsertion(
    id: UUID,
    taxRateId: UUID,
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
  )
