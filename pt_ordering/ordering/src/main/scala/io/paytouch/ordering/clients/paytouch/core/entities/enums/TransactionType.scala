package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait TransactionType extends EnumEntrySnake

case object TransactionType extends Enum[TransactionType] {

  case object Payment extends TransactionType
  case object Refund extends TransactionType
  case object Void extends TransactionType
  case object PreauthPayment extends TransactionType
  case object PreauthVoid extends TransactionType

  val values = findValues
}
