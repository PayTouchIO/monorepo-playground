package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.{ EnumEntrySnake, PaymentProcessorCallbackStatus }

sealed trait CardTransactionResultType extends EnumEntrySnake

case object CardTransactionResultType extends Enum[CardTransactionResultType] {

  case object PartialApproval extends CardTransactionResultType
  case object Approved extends CardTransactionResultType
  case object Declined extends CardTransactionResultType

  val values = findValues

  def fromPaymentProcessorCallbackStatus(paymentProcessorCallbackStatus: PaymentProcessorCallbackStatus) =
    paymentProcessorCallbackStatus match {
      case PaymentProcessorCallbackStatus.Success => Approved
      case _                                      => Declined
    }
}
