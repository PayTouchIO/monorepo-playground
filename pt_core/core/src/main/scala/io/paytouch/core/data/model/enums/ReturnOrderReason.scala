package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReturnOrderReason extends EnumEntrySnake

case object ReturnOrderReason extends Enum[ReturnOrderReason] {

  case object Damaged extends ReturnOrderReason
  case object DidNotOrder extends ReturnOrderReason
  case object Wrong extends ReturnOrderReason
  case object Other extends ReturnOrderReason

  val values = findValues
}
