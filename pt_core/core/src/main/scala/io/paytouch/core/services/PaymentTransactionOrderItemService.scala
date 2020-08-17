package io.paytouch.core.services

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.conversions.PaymentTransactionOrderItemConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.PaymentTransactionOrderItemUpdate
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.{ PaymentTransactionOrderItemValidator, RecoveredOrderUpsertion }

class PaymentTransactionOrderItemService(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends PaymentTransactionOrderItemConversions {
  protected val dao = daos.paymentTransactionOrderItemDao
  protected val validator = new PaymentTransactionOrderItemValidator

  def recoverPaymentTransactionOrderItemUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[PaymentTransactionOrderItemUpdate]] =
    validator
      .validateUpsertion(upsertion)
      .mapNested(_ => toPaymentTransactionOrderItemUpdates(upsertion))
      .map { validation =>
        val description = "While converting payment transaction order items from recovered order upsertion"
        logger.loggedRecoverSeq(validation)(description, upsertion)
      }
}
