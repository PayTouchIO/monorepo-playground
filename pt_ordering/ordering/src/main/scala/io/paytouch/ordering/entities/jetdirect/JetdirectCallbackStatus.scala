package io.paytouch.ordering.entities.jetdirect

import enumeratum._
import io.paytouch.ordering.entities.enums.{ EnumEntrySnake, PaymentProcessorCallbackStatus }

sealed abstract class JetdirectCallbackStatus(val genericStatus: PaymentProcessorCallbackStatus) extends EnumEntrySnake

case object JetdirectCallbackStatus extends Enum[JetdirectCallbackStatus] {

  case object Approved extends JetdirectCallbackStatus(PaymentProcessorCallbackStatus.Success)
  case object Declined extends JetdirectCallbackStatus(PaymentProcessorCallbackStatus.Declined)

  val values = findValues
}
