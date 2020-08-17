package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CashDrawerManagementMode extends EnumEntrySnake

case object CashDrawerManagementMode extends Enum[CashDrawerManagementMode] {

  case object Disabled extends CashDrawerManagementMode
  case object Locked extends CashDrawerManagementMode
  case object Unlocked extends CashDrawerManagementMode

  val values = findValues
}
