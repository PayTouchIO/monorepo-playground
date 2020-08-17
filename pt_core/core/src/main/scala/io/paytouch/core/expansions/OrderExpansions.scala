package io.paytouch.core.expansions

final case class OrderExpansions(
    withGiftCardPasses: Boolean,
    withPaymentTransactions: Boolean,
    withSalesSummary: Boolean,
    withTypeSummary: Boolean,
    withItems: Boolean,
    withLoyaltyPoints: Boolean = false,
    withTickets: Boolean = false,
  ) extends BaseExpansions

object OrderExpansions {

  def withFullOrderItems: OrderExpansions =
    withOrderItems(withGiftCardPasses = true, withPaymentTransactions = true, withLoyaltyPoints = true)

  def withOrderItems(
      withGiftCardPasses: Boolean = false,
      withPaymentTransactions: Boolean = false,
      withLoyaltyPoints: Boolean = false,
      withTickets: Boolean = false,
    ): OrderExpansions =
    OrderExpansions(
      withGiftCardPasses = withGiftCardPasses,
      withPaymentTransactions = withPaymentTransactions,
      withSalesSummary = false,
      withTypeSummary = false,
      withItems = true,
      withLoyaltyPoints = withLoyaltyPoints,
      withTickets = withTickets,
    )

  def withoutOrderItems(
      withGiftCardPasses: Boolean,
      withPaymentTransactions: Boolean,
      withSalesSummary: Boolean,
      withTypeSummary: Boolean,
      withTickets: Boolean,
    ): OrderExpansions =
    OrderExpansions(
      withGiftCardPasses = withGiftCardPasses,
      withPaymentTransactions = withPaymentTransactions,
      withSalesSummary = withSalesSummary,
      withTypeSummary = withTypeSummary,
      withItems = true,
      withTickets = withTickets,
    )

  def empty: OrderExpansions = OrderExpansions(false, false, false, false, false, false, false)

}
