package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.PaymentTransactionFeeType

final case class PaymentTransactionFeeUpsertion(
    id: UUID,
    name: String,
    `type`: PaymentTransactionFeeType,
    amount: BigDecimal,
  )
