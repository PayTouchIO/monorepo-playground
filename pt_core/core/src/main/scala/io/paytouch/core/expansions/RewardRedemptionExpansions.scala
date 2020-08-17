package io.paytouch.core.expansions

final case class RewardRedemptionExpansions(withLoyaltyMembership: Boolean = false) extends BaseExpansions

object RewardRedemptionExpansions {
  def withLoyaltyMembership = RewardRedemptionExpansions(withLoyaltyMembership = true)
}
