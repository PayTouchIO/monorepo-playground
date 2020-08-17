package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CardTransactionStatusType extends EnumEntrySnake

case object CardTransactionStatusType extends Enum[CardTransactionStatusType] {

  case object Committed extends CardTransactionStatusType
  case object Uncommitted extends CardTransactionStatusType
  case object Void extends CardTransactionStatusType
  case object UncommittedVoid extends CardTransactionStatusType

  val values = findValues
}
