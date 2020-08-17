package io.paytouch.core.expansions

final case class LoyaltyProgramExpansions(withLocations: Boolean) extends BaseExpansions

object LoyaltyProgramExpansions {
  def empty: LoyaltyProgramExpansions = LoyaltyProgramExpansions(withLocations = false)
}
