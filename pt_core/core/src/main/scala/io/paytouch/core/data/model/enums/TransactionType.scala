package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed trait TransactionType extends EnumEntrySnake {
  final def isPayment: Boolean =
    this == TransactionType.Payment

  final def isNull: Boolean =
    TransactionType.isNull(this)
}

case object TransactionType extends Enum[TransactionType] {
  case object Payment extends TransactionType
  case object Refund extends TransactionType
  case object Void extends TransactionType
  case object PreauthPayment extends TransactionType
  case object PreauthVoid extends TransactionType

  val values = findValues

  val isPositive: Set[TransactionType] =
    Set(Payment, PreauthPayment)

  val isNull: Set[TransactionType] =
    Set(Refund, Void)
}
