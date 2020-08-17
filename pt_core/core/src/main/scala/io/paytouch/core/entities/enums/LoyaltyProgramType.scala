package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait LoyaltyProgramType extends EnumEntrySnake

case object LoyaltyProgramType extends Enum[LoyaltyProgramType] {

  case object Frequency extends LoyaltyProgramType
  case object Spend extends LoyaltyProgramType

  val values = findValues
}
