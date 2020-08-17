package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait PassType extends EnumEntrySnake

case object PassType extends Enum[PassType] {

  case object Ios extends PassType
  case object Android extends PassType

  val values = findValues
}
