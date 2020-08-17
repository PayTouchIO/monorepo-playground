package io.paytouch.core.entities

import io.paytouch.core.entities.enums.LoyaltyPointsMode

final case class LoyaltyPoints(amount: Int, mode: LoyaltyPointsMode)

object LoyaltyPoints {
  def actual(points: Int): LoyaltyPoints = LoyaltyPoints(amount = points, mode = LoyaltyPointsMode.Actual)

  def potential(points: Int): LoyaltyPoints = LoyaltyPoints(amount = points, mode = LoyaltyPointsMode.Potential)
}
