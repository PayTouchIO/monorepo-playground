package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TipsHandlingMode extends EnumEntrySnake

case object TipsHandlingMode extends Enum[TipsHandlingMode] {

  case object Disabled extends TipsHandlingMode
  case object TipJar extends TipsHandlingMode
  case object EmployeeDriven extends TipsHandlingMode

  val values = findValues
}
