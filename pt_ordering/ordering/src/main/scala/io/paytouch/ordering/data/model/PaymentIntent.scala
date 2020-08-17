package io.paytouch.ordering.data.model

import java.time.{ LocalTime, ZonedDateTime }
import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.Uri

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ PaymentIntentStatus, PaymentMethodType }

final case class PaymentIntentRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    orderItemIds: Seq[UUID],
    subtotalAmount: BigDecimal,
    taxAmount: BigDecimal,
    tipAmount: BigDecimal,
    totalAmount: BigDecimal,
    paymentMethodType: PaymentMethodType,
    successReturnUrl: Uri,
    failureReturnUrl: Uri,
    status: PaymentIntentStatus,
    metadata: PaymentIntentMetadata,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

final case class PaymentIntentUpdate(
    id: Default[UUID],
    merchantId: Option[UUID] = None,
    orderId: Option[UUID] = None,
    orderItemIds: Option[Seq[UUID]] = None,
    subtotalAmount: Option[BigDecimal] = None,
    taxAmount: Option[BigDecimal] = None,
    tipAmount: Option[BigDecimal] = None,
    totalAmount: Option[BigDecimal] = None,
    paymentMethodType: Option[PaymentMethodType] = None,
    successReturnUrl: Option[Uri] = None,
    failureReturnUrl: Option[Uri] = None,
    status: Option[PaymentIntentStatus] = None,
    metadata: Option[PaymentIntentMetadata] = None,
  ) extends SlickUpdate[PaymentIntentRecord] {

  def toRecord: PaymentIntentRecord = {
    requires(
      "merchant_id" -> merchantId,
      "order_id" -> orderId,
      "order_item_ids" -> orderItemIds,
      "subtotal_amount" -> subtotalAmount,
      "tax_amount" -> taxAmount,
      "tip_amount" -> tipAmount,
      "total_amount" -> totalAmount,
      "payment_method_type" -> paymentMethodType,
      "success_return_url" -> successReturnUrl,
      "failure_return_url" -> failureReturnUrl,
      "status" -> status,
      "metadata" -> metadata,
    )

    PaymentIntentRecord(
      id = id.getOrDefault,
      merchantId = merchantId.get,
      orderId = orderId.get,
      orderItemIds = orderItemIds.get,
      subtotalAmount = subtotalAmount.get,
      taxAmount = taxAmount.get,
      tipAmount = tipAmount.get,
      totalAmount = totalAmount.get,
      paymentMethodType = paymentMethodType.get,
      successReturnUrl = successReturnUrl.get,
      failureReturnUrl = failureReturnUrl.get,
      status = status.get,
      metadata = metadata.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PaymentIntentRecord): PaymentIntentRecord =
    PaymentIntentRecord(
      id = record.id,
      merchantId = record.merchantId,
      orderId = record.orderId,
      orderItemIds = record.orderItemIds,
      subtotalAmount = record.subtotalAmount,
      taxAmount = record.taxAmount,
      tipAmount = record.tipAmount,
      totalAmount = record.totalAmount,
      paymentMethodType = record.paymentMethodType,
      successReturnUrl = record.successReturnUrl,
      failureReturnUrl = record.failureReturnUrl,
      status = status.getOrElse(record.status),
      metadata = record.metadata,
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
