package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.PaymentTransactionFeeType

final case class PaymentTransactionFeeRecord(
    id: UUID,
    merchantId: UUID,
    paymentTransactionId: UUID,
    name: String,
    `type`: PaymentTransactionFeeType,
    amount: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class PaymentTransactionFeeUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    paymentTransactionId: Option[UUID],
    name: Option[String],
    `type`: Option[PaymentTransactionFeeType],
    amount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[PaymentTransactionFeeRecord] {

  def toRecord: PaymentTransactionFeeRecord = {
    require(merchantId.isDefined, s"Impossible to convert PaymentTransactionFeeUpdate without a merchant id. [$this]")
    require(
      paymentTransactionId.isDefined,
      s"Impossible to convert PaymentTransactionFeeUpdate without a payment transaction id. [$this]",
    )
    require(merchantId.isDefined, s"Impossible to convert PaymentTransactionFeeUpdate without a name. [$this]")
    require(merchantId.isDefined, s"Impossible to convert PaymentTransactionFeeUpdate without a type. [$this]")
    require(merchantId.isDefined, s"Impossible to convert PaymentTransactionFeeUpdate without an amount. [$this]")
    PaymentTransactionFeeRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      paymentTransactionId = paymentTransactionId.get,
      name = name.get,
      `type` = `type`.get,
      amount = amount.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PaymentTransactionFeeRecord): PaymentTransactionFeeRecord =
    PaymentTransactionFeeRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      paymentTransactionId = paymentTransactionId.getOrElse(record.paymentTransactionId),
      name = name.getOrElse(record.name),
      `type` = `type`.getOrElse(record.`type`),
      amount = amount.getOrElse(record.amount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
