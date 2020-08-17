package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class PaymentTransactionOrderItemRecord(
    id: UUID,
    merchantId: UUID,
    paymentTransactionId: UUID,
    orderItemId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class PaymentTransactionOrderItemUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    paymentTransactionId: Option[UUID],
    orderItemId: Option[UUID],
  ) extends SlickMerchantUpdate[PaymentTransactionOrderItemRecord] {

  def toRecord: PaymentTransactionOrderItemRecord = {
    require(
      merchantId.isDefined,
      s"Impossible to convert PaymentTransactionOrderItemUpdate without a merchant id. [$this]",
    )
    require(
      paymentTransactionId.isDefined,
      s"Impossible to convert PaymentTransactionOrderItemUpdate without a paymentTransaction id. [$this]",
    )
    require(
      orderItemId.isDefined,
      s"Impossible to convert PaymentTransactionOrderItemUpdate without a orderItem id. [$this]",
    )
    PaymentTransactionOrderItemRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      paymentTransactionId = paymentTransactionId.get,
      orderItemId = orderItemId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PaymentTransactionOrderItemRecord): PaymentTransactionOrderItemRecord =
    PaymentTransactionOrderItemRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      paymentTransactionId = paymentTransactionId.getOrElse(record.paymentTransactionId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
