package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.validators.{ RecoveredOrderUpsertion, RecoveredPaymentTransactionUpsertion }

trait PaymentTransactionOrderItemConversions {

  def toPaymentTransactionOrderItemUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Seq[PaymentTransactionOrderItemUpdate] =
    for {
      paymentTransaction <- upsertion.paymentTransactions
      orderItemId <- paymentTransaction.orderItemIds
    } yield toPaymentTransactionOrderItemUpdate(paymentTransaction, orderItemId)

  def toPaymentTransactionOrderItemUpdate(
      paymentTransaction: RecoveredPaymentTransactionUpsertion,
      orderItemId: UUID,
    )(implicit
      user: UserContext,
    ): PaymentTransactionOrderItemUpdate =
    PaymentTransactionOrderItemUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      paymentTransactionId = Some(paymentTransaction.id),
      orderItemId = Some(orderItemId),
    )
}
