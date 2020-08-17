package io.paytouch.ordering.entities.worldpay

import enumeratum._
import io.paytouch.ordering.entities.enums.{ EnumEntrySnake, PaymentProcessorCallbackStatus }

sealed abstract class WorldpayPaymentStatus(val genericStatus: PaymentProcessorCallbackStatus) extends EnumEntrySnake

case object WorldpayPaymentStatus extends Enum[WorldpayPaymentStatus] {

  case object Submitted extends WorldpayPaymentStatus(PaymentProcessorCallbackStatus.Pending)
  case object Complete extends WorldpayPaymentStatus(PaymentProcessorCallbackStatus.Success)
  case object Cancelled extends WorldpayPaymentStatus(PaymentProcessorCallbackStatus.Failure)

  val values = findValues
}
