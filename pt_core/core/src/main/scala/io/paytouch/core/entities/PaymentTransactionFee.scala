package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.PaymentTransactionFeeType

final case class PaymentTransactionFee(
    id: UUID,
    name: String,
    `type`: PaymentTransactionFeeType,
    amount: BigDecimal,
  )

final case class PaymentTransactionFeeUpsertion(
    id: UUID,
    name: String,
    `type`: PaymentTransactionFeeType,
    amount: BigDecimal,
  )
