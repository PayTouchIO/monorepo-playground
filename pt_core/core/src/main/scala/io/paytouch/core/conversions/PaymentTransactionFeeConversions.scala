package io.paytouch.core.conversions

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ UserContext, PaymentTransactionFee => PaymentTransactionFeeEntity }
import io.paytouch.core.validators.RecoveredPaymentTransactionUpsertion

trait PaymentTransactionFeeConversions {

  def fromRecordsToEntities(records: Seq[PaymentTransactionFeeRecord]) =
    records.map(fromRecordToEntity)

  def fromRecordToEntity(record: PaymentTransactionFeeRecord): PaymentTransactionFeeEntity =
    PaymentTransactionFeeEntity(
      id = record.id,
      name = record.name,
      `type` = record.`type`,
      amount = record.amount,
    )

  def toPaymentTransactionFeeUpdates(
      paymentTransactions: Seq[RecoveredPaymentTransactionUpsertion],
    )(implicit
      user: UserContext,
    ): Seq[PaymentTransactionFeeUpdate] =
    paymentTransactions.flatMap(toPaymentTransactionFeeUpdate)

  private def toPaymentTransactionFeeUpdate(
      paymentTransaction: RecoveredPaymentTransactionUpsertion,
    )(implicit
      user: UserContext,
    ) =
    paymentTransaction.fees.map { fee =>
      PaymentTransactionFeeUpdate(
        id = Some(fee.id),
        merchantId = Some(user.merchantId),
        paymentTransactionId = Some(paymentTransaction.id),
        name = Some(fee.name),
        `type` = Some(fee.`type`),
        amount = Some(fee.amount),
      )
    }
}
