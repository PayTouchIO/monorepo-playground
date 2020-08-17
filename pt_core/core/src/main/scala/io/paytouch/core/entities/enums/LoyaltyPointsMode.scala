package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait LoyaltyPointsMode extends EnumEntrySnake

case object LoyaltyPointsMode extends Enum[LoyaltyPointsMode] {

  case object Actual extends LoyaltyPointsMode
  case object Potential extends LoyaltyPointsMode

  val values = findValues
}
