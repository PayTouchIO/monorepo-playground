package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait OrderPaymentType extends EnumEntrySnake

case object OrderPaymentType extends Enum[OrderPaymentType] {

  case object Cash extends OrderPaymentType
  case object CreditCard extends OrderPaymentType
  case object DebitCard extends OrderPaymentType

  val values = findValues
}
