package io.paytouch.ordering.entities.enums

import enumeratum._

sealed trait PaymentMethodType extends EnumEntrySnake

case object PaymentMethodType extends Enum[PaymentMethodType] {
  case object Cash extends PaymentMethodType

  case object Ekashu extends PaymentMethodType
  case object Jetdirect extends PaymentMethodType
  case object Worldpay extends PaymentMethodType
  case object Stripe extends PaymentMethodType

  val values = findValues

  implicit val ord: Ordering[PaymentMethodType] =
    Ordering.by(values.indexOf)
}
