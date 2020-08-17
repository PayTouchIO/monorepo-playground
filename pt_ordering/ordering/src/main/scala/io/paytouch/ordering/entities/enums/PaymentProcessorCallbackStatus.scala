package io.paytouch.ordering.entities.enums

import enumeratum._

sealed trait PaymentProcessorCallbackStatus extends EnumEntrySnake

case object PaymentProcessorCallbackStatus extends Enum[PaymentProcessorCallbackStatus] {

  case object Pending extends PaymentProcessorCallbackStatus
  case object Success extends PaymentProcessorCallbackStatus
  case object Declined extends PaymentProcessorCallbackStatus
  case object Failure extends PaymentProcessorCallbackStatus

  val values = findValues
}
