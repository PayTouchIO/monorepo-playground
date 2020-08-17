package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TimeOffType extends EnumEntrySnake

case object TimeOffType extends Enum[TimeOffType] {

  case object Holiday extends TimeOffType
  case object Other extends TimeOffType
  case object Personal extends TimeOffType
  case object Sick extends TimeOffType
  case object Vacation extends TimeOffType

  val values = findValues
}
