package io.paytouch.core.entities

final case class ReportProfitSummary(
    profit: MonetaryAmount,
    profitPreviousWeek: Option[MonetaryAmount],
    profitPreviousMonth: Option[MonetaryAmount],
  )
