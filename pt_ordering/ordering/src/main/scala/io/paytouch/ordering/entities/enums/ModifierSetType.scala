package io.paytouch.ordering.entities.enums

import enumeratum._

sealed trait ModifierSetType extends EnumEntrySnake

case object ModifierSetType extends Enum[ModifierSetType] {

  case object Hold extends ModifierSetType
  case object Addon extends ModifierSetType
  case object Neutral extends ModifierSetType

  val values = findValues
}
