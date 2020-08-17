package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class CustomerTotals(
    id: UUID,
    totalSpend: MonetaryAmount,
    totalVisits: Int,
  )
