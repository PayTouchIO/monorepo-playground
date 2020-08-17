package io.paytouch.core.reports.entities

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount

final case class CustomerTop(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
    profit: MonetaryAmount,
    spend: MonetaryAmount,
    margin: BigDecimal,
    visits: Int,
  )
