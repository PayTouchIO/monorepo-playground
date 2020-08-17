package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.TransactionType
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ Order, PaymentTransaction, UserContext }

final case class PrepareOrderReceipt(
    eventName: String = PrepareOrderReceipt.eventName,
    payload: PrepareOrderReceiptPayload,
  ) extends PtCoreMsg[Order]

final case class PrepareOrderReceiptPayload(
    `object`: ExposedName,
    data: Order,
    recipientEmail: String,
    merchantId: UUID,
    paymentTransactionId: Option[UUID],
    userContext: UserContext,
  ) extends EmailEntityPayloadLike[Order]

object PrepareOrderReceipt {

  val eventName = "prepare_order_receipt"

  def apply(
      order: Order,
      paymentTransactionId: Option[UUID],
      recipientEmail: String,
    )(implicit
      user: UserContext,
    ): PrepareOrderReceipt = {
    val filteredOrder = filterOrderPaymentTransactionsToDeprecate(order, paymentTransactionId)
    PrepareOrderReceipt(
      eventName,
      PrepareOrderReceiptPayload(
        order.classShortName,
        filteredOrder,
        recipientEmail,
        user.merchantId,
        paymentTransactionId,
        user,
      ),
    )
  }

  // this should be replaced by 'filterOrderPaymentTransactions' asap OrderReceiptRequestV2 is the only version in use
  private def filterOrderPaymentTransactionsToDeprecate(order: Order, paymentTransactionId: Option[UUID]): Order = {
    val filteredPaymentTransactions = paymentTransactionId match {
      case Some(id) => order.paymentTransactions.map(_.filter(_.id == id))
      case None     => order.paymentTransactions
    }
    order.copy(paymentTransactions = filteredPaymentTransactions)
  }

  def filterOrderPaymentTransactions(order: Order, paymentTransactionId: Option[UUID]): Order = {
    val filteredPaymentTransactions = paymentTransactionId match {
      case Some(id) => order.paymentTransactions.map(_.filter(_.id == id))
      case None     => order.paymentTransactions.map(_.filterOutRefunds.filterOutVoids)
    }
    order.copy(paymentTransactions = filteredPaymentTransactions)
  }

  implicit class RichSeqPaymentTransactions(transactions: Seq[PaymentTransaction]) {

    def filterOutRefunds: Seq[PaymentTransaction] = filterOut(TransactionType.Refund)
    def filterOutVoids: Seq[PaymentTransaction] = filterOut(TransactionType.Void)

    private def filterOut(`type`: TransactionType): Seq[PaymentTransaction] =
      transactions.filterNot(_.`type`.contains(`type`))

  }

}
