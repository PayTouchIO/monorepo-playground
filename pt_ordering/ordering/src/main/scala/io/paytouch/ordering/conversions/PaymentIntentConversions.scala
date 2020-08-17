package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.calculations.PaymentIntentCalculationsResult
import io.paytouch.ordering.data.model.{ PaymentIntentRecord, PaymentIntentUpdate => PaymentIntentUpdateModel }
import io.paytouch.ordering.entities.{
  AppContext => Context,
  PaymentIntent => PaymentIntentEntity,
  PaymentIntentUpsertion,
  MonetaryAmount,
}
import io.paytouch.ordering.entities.enums.PaymentIntentStatus
import io.paytouch.ordering.clients.paytouch.core.entities.PaymentTransactionUpsertion

trait PaymentIntentConversions {
  protected def fromRecordToEntity(
      record: PaymentIntentRecord,
    )(implicit
      context: Context,
    ): PaymentIntentEntity =
    PaymentIntentEntity(
      id = record.id,
      merchantId = record.merchantId,
      orderId = record.orderId,
      orderItemIds = record.orderItemIds,
      subtotal = MonetaryAmount(record.subtotalAmount),
      tax = MonetaryAmount(record.taxAmount),
      tip = MonetaryAmount(record.tipAmount),
      total = MonetaryAmount(record.totalAmount),
      paymentMethodType = record.paymentMethodType,
      // Payment processor data in enriched in server
      paymentProcessorData = None,
      metadata = record.metadata,
      status = record.status,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  protected def creationToUpsertionModel(
      id: UUID,
      update: PaymentIntentUpsertion,
      calculations: PaymentIntentCalculationsResult,
    )(implicit
      context: Context,
    ): PaymentIntentUpdateModel =
    PaymentIntentUpdateModel(
      id = id,
      merchantId = Some(update.merchantId),
      orderId = Some(update.orderId),
      orderItemIds = Some(update.orderItemIds),
      tipAmount = Some(calculations.tipAmount),
      taxAmount = Some(calculations.taxAmount),
      subtotalAmount = Some(calculations.subtotalAmount),
      totalAmount = Some(calculations.totalAmount),
      paymentMethodType = Some(update.paymentMethodType),
      successReturnUrl = Some(update.successReturnUrl),
      failureReturnUrl = Some(update.failureReturnUrl),
      status = Some(PaymentIntentStatus.New),
      metadata = Some(update.metadata),
    )

  protected def updateToUpsertionModel(
      id: UUID,
      update: PaymentIntentUpsertion,
    )(implicit
      context: Context,
    ): PaymentIntentUpdateModel =
    PaymentIntentUpdateModel(
      id = id,
      // TODO
      status = Some(PaymentIntentStatus.New),
      // Not changed
      merchantId = None,
      orderId = None,
      orderItemIds = None,
      tipAmount = None,
      taxAmount = None,
      subtotalAmount = None,
      totalAmount = None,
      paymentMethodType = None,
      successReturnUrl = None,
      failureReturnUrl = None,
      metadata = None,
    )

  protected def paymentTransactionWithExtras(
      paymentIntent: PaymentIntentRecord,
      paymentTransaction: PaymentTransactionUpsertion,
    ): PaymentTransactionUpsertion =
    paymentTransaction.copy(
      orderItemIds = paymentIntent.orderItemIds,
      paymentDetails = paymentTransaction
        .paymentDetails
        .copy(
          tipAmount = paymentIntent.tipAmount,
        ),
    )
}
