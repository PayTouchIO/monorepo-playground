package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum.Enum
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait PaymentTransactionFeeType extends EnumEntrySnake

case object PaymentTransactionFeeType extends Enum[PaymentTransactionFeeType] {

  case object JetpayFee extends PaymentTransactionFeeType
  case object JetpayCashDiscount extends PaymentTransactionFeeType

  val values = findValues
}
