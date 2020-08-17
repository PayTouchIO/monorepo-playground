package io.paytouch.core.expansions

final case class TicketExpansions(withOrder: Boolean) extends BaseExpansions

object TicketExpansions {
  def empty = TicketExpansions(false)
}
