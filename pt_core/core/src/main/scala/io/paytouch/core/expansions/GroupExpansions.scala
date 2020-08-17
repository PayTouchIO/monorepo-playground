package io.paytouch.core.expansions

final case class GroupExpansions(
    withCustomers: Boolean,
    withCustomersCount: Boolean,
    withRevenue: Boolean,
    withVisits: Boolean,
  ) extends BaseExpansions
object GroupExpansions {

  def apply(withCustomers: Boolean): GroupExpansions =
    GroupExpansions(withCustomers = withCustomers, withCustomersCount = false, withRevenue = false, withVisits = false)
}
