package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class PaymentStatus extends EnumEntrySnake {
  def isReturn: Boolean =
    PaymentStatus.isReturn(this)

  def isPending: Boolean =
    this == PaymentStatus.Pending

  def isPositive: Boolean =
    PaymentStatus.isPositive(this)
}

case object PaymentStatus extends Enum[PaymentStatus] {
  case object Pending extends PaymentStatus
  case object Paid extends PaymentStatus
  case object PartiallyPaid extends PaymentStatus
  case object PartiallyRefunded extends PaymentStatus
  case object Refunded extends PaymentStatus
  case object Voided extends PaymentStatus

  override val values =
    findValues

  val isReturn: Set[PaymentStatus] =
    Set(Refunded, Voided)

  val isPositive: Set[PaymentStatus] =
    Set(Paid, PartiallyPaid, PartiallyRefunded)

  val isPaid: Set[PaymentStatus] =
    isPositive + Refunded
}
