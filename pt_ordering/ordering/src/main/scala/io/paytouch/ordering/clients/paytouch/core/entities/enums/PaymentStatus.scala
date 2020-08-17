package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait PaymentStatus extends EnumEntrySnake

case object PaymentStatus extends Enum[PaymentStatus] {

  case object Pending extends PaymentStatus
  case object Paid extends PaymentStatus
  case object PartiallyPaid extends PaymentStatus
  case object PartiallyRefunded extends PaymentStatus
  case object Refunded extends PaymentStatus
  case object Voided extends PaymentStatus

  val values = findValues
}
