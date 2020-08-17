package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed trait TransactionPaymentType extends EnumEntrySnake {
  final def toOrderPaymentType: OrderPaymentType =
    this match {
      case TransactionPaymentType.Cash             => OrderPaymentType.Cash
      case TransactionPaymentType.Check            => OrderPaymentType.Check
      case TransactionPaymentType.CreditCard       => OrderPaymentType.CreditCard
      case TransactionPaymentType.DebitCard        => OrderPaymentType.DebitCard
      case TransactionPaymentType.DeliveryProvider => OrderPaymentType.DeliveryProvider
      case TransactionPaymentType.GiftCard         => OrderPaymentType.GiftCard
    }
  final def isCard: Boolean = TransactionPaymentType.cardValues.contains(this)
}

case object TransactionPaymentType extends Enum[TransactionPaymentType] {
  case object Cash extends TransactionPaymentType
  case object Check extends TransactionPaymentType
  case object CreditCard extends TransactionPaymentType
  case object DebitCard extends TransactionPaymentType
  case object DeliveryProvider extends TransactionPaymentType
  case object GiftCard extends TransactionPaymentType

  val values = findValues

  val reportValues = Seq(
    Cash,
    CreditCard,
    DebitCard,
    GiftCard,
  )

  val cardValues = Seq(CreditCard, DebitCard)
}
