package io.paytouch.ordering.clients.worldpay.entities

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType

trait WorldpayResponse

final case class ErrorResponse(status: ResponseCode) extends WorldpayResponse

final case class TransactionSetupResponse(transactionSetupId: String) extends WorldpayResponse

final case class TransactionQueryResponse(
    accountId: String,
    applicationId: String,
    approvalNumber: String,
    approvedAmount: BigDecimal,
    maskedCardNumber: String,
    cardType: CardType,
    cardHolderName: String,
    terminalId: String,
    transactionId: String,
    transactionSetupId: String,
    hostResponseCode: String,
  ) extends WorldpayResponse
