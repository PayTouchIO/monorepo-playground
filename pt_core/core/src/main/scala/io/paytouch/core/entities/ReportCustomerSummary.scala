package io.paytouch.core.entities

final case class ReportCustomerSummary(`new`: CustomersSummary, returning: CustomersSummary)

final case class CustomersSummary(count: Int, spend: MonetaryAmount)
