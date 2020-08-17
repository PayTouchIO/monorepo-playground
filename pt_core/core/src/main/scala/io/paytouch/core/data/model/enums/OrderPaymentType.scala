package io.paytouch.core.data.model.enums

import cats.implicits._

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class OrderPaymentType extends EnumEntrySnake {
  final def toTransactionPaymentType: Option[TransactionPaymentType] =
    this match {
      case OrderPaymentType.Cash             => TransactionPaymentType.Cash.some
      case OrderPaymentType.Check            => TransactionPaymentType.Check.some
      case OrderPaymentType.CreditCard       => TransactionPaymentType.CreditCard.some
      case OrderPaymentType.DebitCard        => TransactionPaymentType.DebitCard.some
      case OrderPaymentType.DeliveryProvider => TransactionPaymentType.DeliveryProvider.some
      case OrderPaymentType.GiftCard         => TransactionPaymentType.GiftCard.some
      case _                                 => none
    }
}

case object OrderPaymentType extends Enum[OrderPaymentType] {
  case object Cash extends OrderPaymentType
  case object CreditCard extends OrderPaymentType
  case object DebitCard extends OrderPaymentType
  case object Check extends OrderPaymentType
  case object GiftCard extends OrderPaymentType
  case object Split extends OrderPaymentType
  case object DeliveryProvider extends OrderPaymentType

  val values = findValues
}
