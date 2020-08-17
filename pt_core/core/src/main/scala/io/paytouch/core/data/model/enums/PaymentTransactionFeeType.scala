package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait PaymentTransactionFeeType extends EnumEntrySnake

case object PaymentTransactionFeeType extends Enum[PaymentTransactionFeeType] {

  // Deprecated
  case object PaxFee extends PaymentTransactionFeeType
  case object PaxCashDiscount extends PaymentTransactionFeeType

  case object JetpayFee extends PaymentTransactionFeeType
  case object JetpayCashDiscount extends PaymentTransactionFeeType

  case object DeliveryProviderFee extends PaymentTransactionFeeType

  val values = findValues
}
