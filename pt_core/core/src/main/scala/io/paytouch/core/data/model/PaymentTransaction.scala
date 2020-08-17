package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{
  PaymentProcessor,
  TransactionPaymentProcessor,
  TransactionPaymentType,
  TransactionType,
}
import io.paytouch.core.entities.PaymentDetails

final case class PaymentTransactionRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    customerId: Option[UUID],
    `type`: Option[TransactionType],
    refundedPaymentTransactionId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    paymentDetails: Option[PaymentDetails],
    version: Int,
    paidAt: Option[ZonedDateTime],
    paymentProcessor: TransactionPaymentProcessor,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class PaymentTransactionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
    customerId: Option[UUID],
    `type`: Option[TransactionType],
    refundedPaymentTransactionId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    paymentDetails: Option[PaymentDetails],
    version: Option[Int],
    paidAt: Option[ZonedDateTime],
    paymentProcessor: Option[TransactionPaymentProcessor],
  ) extends SlickMerchantUpdate[PaymentTransactionRecord] {
  def toRecord: PaymentTransactionRecord = {
    require(merchantId.isDefined, s"Impossible to convert PaymentTransactionUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert PaymentTransactionUpdate without a order id. [$this]")
    require(version.isDefined, s"Impossible to convert PaymentTransactionUpdate without a version. [$this]")
    require(paymentProcessor.isDefined, s"Impossible to convert PaymentTransactionUpdate without a version. [$this]")
    PaymentTransactionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      customerId = customerId,
      `type` = `type`,
      refundedPaymentTransactionId = refundedPaymentTransactionId,
      paymentType = paymentType,
      paymentDetails = paymentDetails,
      version = version.get,
      paidAt = paidAt,
      paymentProcessor = paymentProcessor.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  // a PaymentTransactionRecord is not updateable
  def updateRecord(record: PaymentTransactionRecord): PaymentTransactionRecord = record
}
