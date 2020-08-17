package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class CardTransactionResultType extends EnumEntrySnake

case object CardTransactionResultType extends Enum[CardTransactionResultType] {
  case object PartialApproval extends CardTransactionResultType
  case object Approved extends CardTransactionResultType
  case object Declined extends CardTransactionResultType

  val values = findValues
}
