package io.paytouch.ordering.clients.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ TransactionPaymentType, TransactionType }
import io.paytouch.ordering.entities.enums.PaymentProcessor

final case class PaymentTransaction(
    `type`: TransactionType,
    paymentProcessorV2: PaymentProcessor,
    paymentType: TransactionPaymentType,
    paymentDetails: GenericPaymentDetails,
    paidAt: ZonedDateTime,
    orderItemIds: Seq[UUID],
  )

final case class PaymentTransactionUpsertion(
    id: UUID,
    `type`: TransactionType,
    paymentProcessorV2: PaymentProcessor,
    paymentType: TransactionPaymentType,
    paymentDetails: GenericPaymentDetails,
    paidAt: ZonedDateTime,
    orderItemIds: Seq[UUID] = Seq.empty,
    fees: Seq[PaymentTransactionFeeUpsertion] = Seq.empty,
    version: Int = 1,
  )

object OrderServiceStorePaymentTransactionUpsertion {
  implicit def toOrderServiceStorePaymentTransactionUpsertion(u: PaymentTransactionUpsertion) =
    OrderServiceStorePaymentTransactionUpsertion(
      id = u.id,
      `type` = u.`type`,
      paymentProcessor = u.paymentProcessorV2,
      paymentType = u.paymentType,
      paymentDetails = u.paymentDetails,
      paidAt = u.paidAt,
      orderItemIds = u.orderItemIds,
      fees = u.fees,
      version = u.version,
    )
}

final case class OrderServiceStorePaymentTransactionUpsertion(
    id: UUID,
    `type`: TransactionType,
    paymentProcessor: PaymentProcessor,
    paymentType: TransactionPaymentType,
    paymentDetails: GenericPaymentDetails,
    paidAt: ZonedDateTime,
    orderItemIds: Seq[UUID],
    fees: Seq[PaymentTransactionFeeUpsertion],
    version: Int,
  )
