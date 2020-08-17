package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait QuantityChangeReason extends EnumEntrySnake

case object QuantityChangeReason extends Enum[QuantityChangeReason] {

  case object CustomerReturn extends QuantityChangeReason
  case object Damaged extends QuantityChangeReason
  case object Manual extends QuantityChangeReason
  case object Receiving extends QuantityChangeReason
  case object Sale extends QuantityChangeReason
  case object Spillage extends QuantityChangeReason
  case object SupplierReturn extends QuantityChangeReason
  case object Transfer extends QuantityChangeReason

  val values = findValues
}
