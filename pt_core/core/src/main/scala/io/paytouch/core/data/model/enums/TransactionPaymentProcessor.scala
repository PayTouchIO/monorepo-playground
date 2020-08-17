package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class TransactionPaymentProcessor extends EnumEntrySnake

case object TransactionPaymentProcessor extends Enum[TransactionPaymentProcessor] {
  case object Creditcall extends TransactionPaymentProcessor
  case object Jetpay extends TransactionPaymentProcessor
  case object Worldpay extends TransactionPaymentProcessor
  case object Stripe extends TransactionPaymentProcessor
  case object Paytouch extends TransactionPaymentProcessor
  case object DeliveryProvider extends TransactionPaymentProcessor

  val values = findValues
}
