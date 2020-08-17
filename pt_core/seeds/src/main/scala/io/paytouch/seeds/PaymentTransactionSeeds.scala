package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object PaymentTransactionSeeds extends Seeds {

  lazy val paymentTransactionDao = daos.paymentTransactionDao

  def load(orders: Seq[OrderRecord])(implicit user: UserRecord): Future[Seq[PaymentTransactionRecord]] = {
    val paymentTransactionIds = paymentTransactionIdsPerEmail(user.email)

    val paymentTransactions = paymentTransactionIds.zip(orders.shuffle).map {
      case (paymentTransactionId, order) =>
        PaymentTransactionUpdate(
          id = Some(paymentTransactionId),
          merchantId = Some(user.merchantId),
          orderId = Some(order.id),
          customerId = order.customerId,
          `type` = Some(genTransactionType.instance),
          refundedPaymentTransactionId = None,
          paymentType = Some(genTransactionPaymentType.instance),
          paymentDetails = None,
          version = Some(2),
          paidAt = Some(genZonedDateTime.instance),
          paymentProcessor = Some(genTransactionPaymentProcessor.instance),
        )
    }

    paymentTransactionDao.bulkUpsert(paymentTransactions).extractRecords
  }
}
