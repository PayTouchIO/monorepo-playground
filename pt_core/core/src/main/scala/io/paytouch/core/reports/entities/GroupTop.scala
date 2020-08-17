package io.paytouch.core.reports.entities

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount

final case class GroupTop(
    id: UUID,
    name: String,
    spend: MonetaryAmount,
    profit: MonetaryAmount,
    margin: BigDecimal,
    visits: Int,
  )
