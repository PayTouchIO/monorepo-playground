package io.paytouch.ordering.entities.enums

import enumeratum._

sealed trait PaymentIntentStatus extends EnumEntrySnake

case object PaymentIntentStatus extends Enum[PaymentIntentStatus] {

  case object New extends PaymentIntentStatus
  case object Paid extends PaymentIntentStatus
  case object Canceled extends PaymentIntentStatus

  val values = findValues
}
