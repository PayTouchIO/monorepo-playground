package io.paytouch.core.expansions

final case class InventoryCountExpansions(withUser: Boolean, withLocation: Boolean) extends BaseExpansions

object InventoryCountExpansions {
  def empty = apply(false, false)
}
