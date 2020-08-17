package io.paytouch.core.validators

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.errors.InvalidOrderItemIds
import io.paytouch.core.utils.Multiple

import scala.concurrent._

class PaymentTransactionOrderItemValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  def validateUpsertion(upsertion: RecoveredOrderUpsertion) =
    Future.successful {
      val orderItemIds = upsertion.items.map(_.id).toSet
      val orderItemIdsInPaymentTransactions = upsertion.paymentTransactions.flatMap(_.orderItemIds).toSet

      val difference = orderItemIdsInPaymentTransactions diff orderItemIds
      if (difference.isEmpty) Multiple.success((): Unit)
      else Multiple.failure(InvalidOrderItemIds(difference.toSeq))
    }

}
