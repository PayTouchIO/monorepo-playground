package io.paytouch.core.entities

final case class ReportSalesSummary(
    revenue: MonetaryAmount,
    count: Int,
    avgSale: MonetaryAmount,
  )

object ReportSalesSummary {

  def apply(revenue: MonetaryAmount, count: Int): ReportSalesSummary =
    apply(revenue = revenue, count = count, avgSale = revenue / count)
}
