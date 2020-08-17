package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait GiftCardPassTransactionType extends EnumEntrySnake

case object GiftCardPassTransactionType extends Enum[GiftCardPassTransactionType] {

  case object Payment extends GiftCardPassTransactionType
  case object Refund extends GiftCardPassTransactionType

  val values = findValues
}
