package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class PaymentProcessor(val transactionPaymentProcessor: TransactionPaymentProcessor)
    extends EnumEntrySnake

case object PaymentProcessor extends Enum[PaymentProcessor] {
  case object Creditcall extends PaymentProcessor(TransactionPaymentProcessor.Creditcall)
  case object Jetpay extends PaymentProcessor(TransactionPaymentProcessor.Jetpay)
  case object Worldpay extends PaymentProcessor(TransactionPaymentProcessor.Worldpay)
  case object Stripe extends PaymentProcessor(TransactionPaymentProcessor.Stripe)
  case object Paytouch extends PaymentProcessor(TransactionPaymentProcessor.Paytouch)

  val values = findValues
}
