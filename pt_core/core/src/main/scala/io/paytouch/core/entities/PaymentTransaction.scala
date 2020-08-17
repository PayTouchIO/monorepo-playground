package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ TransactionPaymentProcessor, TransactionPaymentType, TransactionType }

final case class PaymentTransaction(
    id: UUID,
    orderId: UUID,
    customerId: Option[UUID],
    `type`: Option[TransactionType],
    refundedPaymentTransactionId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    paymentDetails: Option[PaymentDetails],
    paidAt: Option[ZonedDateTime],
    orderItemIds: Seq[UUID] = Seq.empty,
    fees: Seq[PaymentTransactionFee],
    totalBilledToCustomer: Option[MonetaryAmount],
    paymentProcessorV2: TransactionPaymentProcessor,
  )

final case class PaymentTransactionUpsertion(
    id: UUID,
    `type`: Option[TransactionType],
    paymentType: Option[TransactionPaymentType],
    refundedPaymentTransactionId: Option[UUID],
    paymentDetails: Option[PaymentDetails],
    paidAt: Option[ZonedDateTime],
    orderItemIds: Seq[UUID] = Seq.empty,
    fees: Seq[PaymentTransactionFeeUpsertion] = Seq.empty,
    version: Int = 1,
    paymentProcessorV2: Option[TransactionPaymentProcessor],
  )
