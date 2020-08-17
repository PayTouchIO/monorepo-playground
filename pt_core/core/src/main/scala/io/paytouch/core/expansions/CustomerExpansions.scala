package io.paytouch.core.expansions

final case class CustomerExpansions(
    withVisits: Boolean,
    withSpend: Boolean,
    withLocations: Boolean,
    withLoyaltyPrograms: Boolean,
    withAvgTips: Boolean,
    withLoyaltyMemberships: Boolean,
    withBillingDetails: Boolean,
  ) extends BaseExpansions

object CustomerExpansions {
  def forList(
      withVisits: Boolean,
      withSpend: Boolean,
      withLocations: Boolean,
      withLoyaltyPrograms: Boolean,
      withLoyaltyStatuses: Boolean,
      withLoyaltyMemberships: Boolean,
      withBillingDetails: Boolean,
    ): CustomerExpansions =
    CustomerExpansions(
      withVisits = withVisits,
      withSpend = withSpend,
      withLocations = withLocations,
      withLoyaltyPrograms = withLoyaltyPrograms,
      withAvgTips = false,
      withLoyaltyMemberships = withLoyaltyStatuses || withLoyaltyMemberships,
      withBillingDetails = withBillingDetails,
    )

  def forGet(
      withVisits: Boolean,
      withSpend: Boolean,
      withAvgTips: Boolean,
      withLoyaltyStatuses: Boolean,
      withLoyaltyMemberships: Boolean,
      withBillingDetails: Boolean,
    ): CustomerExpansions =
    CustomerExpansions(
      withVisits = withVisits,
      withSpend = withSpend,
      withLocations = false,
      withLoyaltyPrograms = false,
      withAvgTips = withAvgTips,
      withLoyaltyMemberships = withLoyaltyStatuses || withLoyaltyMemberships,
      withBillingDetails = withBillingDetails,
    )

  def empty =
    CustomerExpansions(
      withVisits = false,
      withSpend = false,
      withLocations = false,
      withLoyaltyPrograms = false,
      withAvgTips = false,
      withLoyaltyMemberships = false,
      withBillingDetails = false,
    )

  def all =
    CustomerExpansions(
      withVisits = true,
      withSpend = true,
      withLocations = true,
      withLoyaltyPrograms = true,
      withAvgTips = true,
      withLoyaltyMemberships = true,
      withBillingDetails = true,
    )
}
