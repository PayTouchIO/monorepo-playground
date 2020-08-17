package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ChangeReason extends EnumEntrySnake

case object ChangeReason extends Enum[ChangeReason] {

  case object CostDecrease extends ChangeReason
  case object CostIncrease extends ChangeReason
  case object Discount extends ChangeReason
  case object Manual extends ChangeReason
  case object PriceIncrease extends ChangeReason
  case object PriceDecrease extends ChangeReason
  case object SupplierIncrease extends ChangeReason
  case object SupplierDecrease extends ChangeReason

  val values = findValues
}
