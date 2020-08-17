package io.paytouch.ordering.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch._

final case class GiftCardPassApplied(
    id: GiftCardPass.Id,
    onlineCode: GiftCardPass.OnlineCode.Raw,
    balance: MonetaryAmount,
    addedAt: ZonedDateTime,
    amountToCharge: Option[MonetaryAmount] = None,
    paymentTransactionId: Option[UUID] = None,
  )
