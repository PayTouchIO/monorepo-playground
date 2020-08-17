package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class BusinessType extends EnumEntrySnake

case object BusinessType extends Enum[BusinessType] {
  case object QSR extends BusinessType
  case object Restaurant extends BusinessType
  case object Retail extends BusinessType

  val values = findValues
}
