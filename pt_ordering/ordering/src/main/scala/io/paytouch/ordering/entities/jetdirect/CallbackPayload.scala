package io.paytouch.ordering.entities.jetdirect

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.entities.enums.PaymentProcessorCallbackStatus

final case class CallbackPayload(
    status: PaymentProcessorCallbackStatus,
    responseText: String,
    cid: Option[String],
    name: Option[String],
    card: Option[CardType],
    cardNum: Option[String],
    expandedCardNum: Option[String],
    expDate: Option[String],
    amount: BigDecimal,
    transId: Option[String],
    actCode: Option[String],
    apprCode: Option[String],
    cvvMatch: Option[String],
    addressMatch: Option[String],
    zipMatch: Option[String],
    avsMatch: Option[String],
    ccToken: Option[String],
    customerEmail: Option[String],
    orderNumber: UUID,
    jpReturnHash: String,
    rrn: Option[String],
    uniqueid: Option[String],
    rawResponse: Option[String],
    feeAmount: Option[BigDecimal],
    tipAmount: Option[BigDecimal],
  )
