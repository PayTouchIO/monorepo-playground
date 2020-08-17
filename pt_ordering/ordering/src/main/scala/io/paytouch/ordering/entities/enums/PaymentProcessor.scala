package io.paytouch.ordering.entities.enums

import enumeratum._

sealed abstract class PaymentProcessor extends EnumEntrySnake

case object PaymentProcessor extends Enum[PaymentProcessor] {
  case object Ekashu extends PaymentProcessor
  case object Jetdirect extends PaymentProcessor
  case object Worldpay extends PaymentProcessor
  case object Stripe extends PaymentProcessor
  case object Paytouch extends PaymentProcessor

  val values = findValues
  val withPaymentConfig = values.filterNot(p => Seq(Stripe, Paytouch) contains p)
}
