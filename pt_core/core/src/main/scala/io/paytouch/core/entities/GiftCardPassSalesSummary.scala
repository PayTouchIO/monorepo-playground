package io.paytouch.core.entities

final case class GiftCardPassSalesSummary(
    purchased: GiftCardPassSalesReport,
    used: GiftCardPassSalesReport,
    unused: GiftCardPassSalesReport,
  )

final case class GiftCardPassSalesReport(
    count: Int,
    customers: Int,
    value: MonetaryAmount,
  )

object GiftCardPassSalesReport {
  def apply(
      count: Int,
      customers: Int,
      valueAmount: BigDecimal,
    )(implicit
      user: UserContext,
    ): GiftCardPassSalesReport =
    apply(count, customers, MonetaryAmount(valueAmount))
}
