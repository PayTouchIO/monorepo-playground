package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class ModifierSetType extends EnumEntrySnake

case object ModifierSetType extends Enum[ModifierSetType] {
  case object Hold extends ModifierSetType
  case object Addon extends ModifierSetType
  case object Neutral extends ModifierSetType

  val values = findValues
}
