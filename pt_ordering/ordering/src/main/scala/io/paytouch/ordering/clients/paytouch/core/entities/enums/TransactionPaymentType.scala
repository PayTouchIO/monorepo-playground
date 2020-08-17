package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait TransactionPaymentType extends EnumEntrySnake

case object TransactionPaymentType extends Enum[TransactionPaymentType] {

  case object Cash extends TransactionPaymentType
  case object CreditCard extends TransactionPaymentType
  case object DebitCard extends TransactionPaymentType
  case object Check extends TransactionPaymentType
  case object GiftCard extends TransactionPaymentType
  case object DeliveryProvider extends TransactionPaymentType

  val values = findValues
}
