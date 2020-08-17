package io.paytouch.core.expansions

final case class GiftCardPassExpansions(withTransactions: Boolean) extends BaseExpansions

object GiftCardPassExpansions {
  def empty: GiftCardPassExpansions = GiftCardPassExpansions(withTransactions = false)
}
