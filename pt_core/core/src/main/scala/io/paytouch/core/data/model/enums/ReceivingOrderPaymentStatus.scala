package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReceivingOrderPaymentStatus extends EnumEntrySnake

case object ReceivingOrderPaymentStatus extends Enum[ReceivingOrderPaymentStatus] {

  case object Unpaid extends ReceivingOrderPaymentStatus
  case object Paid extends ReceivingOrderPaymentStatus
  case object Partial extends ReceivingOrderPaymentStatus

  val values = findValues
}
