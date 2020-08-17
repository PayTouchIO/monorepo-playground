package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait KitchenType extends EnumEntrySnake

case object KitchenType extends Enum[KitchenType] {

  case object Bar extends KitchenType
  case object Kitchen extends KitchenType

  val values = findValues
}
