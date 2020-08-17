package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{
  MonetaryAmount,
  PaymentDetails,
  PaymentTransactionFee,
  UserContext,
  PaymentTransaction => PaymentTransactionEntity,
}
import io.paytouch.core.validators.{ RecoveredOrderUpsertion, RecoveredPaymentTransactionUpsertion }

trait PaymentTransactionConversions extends EntityConversion[PaymentTransactionRecord, PaymentTransactionEntity] {

  def fromRecordToEntity(record: PaymentTransactionRecord)(implicit user: UserContext): PaymentTransactionEntity =
    fromRecordAndOptionsToEntity(record, Map.empty, Seq.empty, Seq.empty)

  def fromRecordsAndOptionsToEntities(
      records: Seq[PaymentTransactionRecord],
      lookupIdPerGiftCard: Map[UUID, String],
      paymentTransactionOrderItems: Seq[PaymentTransactionOrderItemRecord],
      feesPerPaymentTransaction: Map[UUID, Seq[PaymentTransactionFee]],
    ): Seq[PaymentTransactionEntity] =
    records.map { record =>
      val orderItemIds = paymentTransactionOrderItems.filter(_.paymentTransactionId == record.id).map(_.orderItemId)
      val fees = feesPerPaymentTransaction.getOrElse(record.id, Seq.empty)
      fromRecordAndOptionsToEntity(record, lookupIdPerGiftCard, orderItemIds, fees)
    }

  def fromRecordAndOptionsToEntity(
      record: PaymentTransactionRecord,
      lookupIdPerGiftCard: Map[UUID, String],
      orderItemIds: Seq[UUID],
      fees: Seq[PaymentTransactionFee],
    ): PaymentTransactionEntity = {
    val totalBilledToCustomer = for {
      details <- record.paymentDetails
      amount <- details.amount
      currency <- details.currency
      cashbackAmount <- details.cashbackAmount.orElse[BigDecimal](Some(0))
      feesSum <- Some(fees.map(_.amount).sum)
      total = amount + cashbackAmount + feesSum
    } yield MonetaryAmount(total, currency)
    PaymentTransactionEntity(
      id = record.id,
      orderId = record.orderId,
      customerId = record.customerId,
      `type` = record.`type`,
      refundedPaymentTransactionId = record.refundedPaymentTransactionId,
      paymentType = record.paymentType,
      paymentDetails = record.paymentDetails.map(toPaymentDetails(_, lookupIdPerGiftCard)),
      paidAt = record.paidAt,
      orderItemIds = orderItemIds,
      fees = fees,
      totalBilledToCustomer = totalBilledToCustomer,
      paymentProcessorV2 = record.paymentProcessor,
    )
  }

  private def toPaymentDetails(paymentDetails: PaymentDetails, lookupIdPerGiftCard: Map[UUID, String]): PaymentDetails =
    paymentDetails.copy(
      giftCardPassLookupId = paymentDetails.giftCardPassId.flatMap(lookupIdPerGiftCard.get),
      worldpay = None,
    )

  def toPaymentTransactionUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Seq[PaymentTransactionUpdate] = {
    val orderId = upsertion.orderId
    val customerId = upsertion.customerId
    upsertion.paymentTransactions.map(toPaymentTransactionUpdate(orderId, customerId, _))
  }

  def toPaymentTransactionUpdate(
      orderId: UUID,
      customerId: Option[UUID],
      paymentTransaction: RecoveredPaymentTransactionUpsertion,
    )(implicit
      user: UserContext,
    ): PaymentTransactionUpdate =
    PaymentTransactionUpdate(
      id = Some(paymentTransaction.id),
      merchantId = Some(user.merchantId),
      orderId = Some(orderId),
      customerId = customerId,
      `type` = paymentTransaction.`type`,
      refundedPaymentTransactionId = paymentTransaction.refundedPaymentTransactionId,
      paymentType = paymentTransaction.paymentType,
      paymentDetails = paymentTransaction.paymentDetails,
      version = Some(paymentTransaction.version),
      paidAt = paymentTransaction.paidAt,
      paymentProcessor = Some(paymentTransaction.paymentProcessor),
    )

  def giftCardIdForTransactions(records: Seq[PaymentTransactionRecord]): Seq[UUID] =
    records.flatMap { record =>
      for {
        paymentDetails <- record.paymentDetails
        giftCardId <- paymentDetails.giftCardPassId
      } yield giftCardId
    }
}
