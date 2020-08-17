package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.PaymentTransactionFeeConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ PaymentTransactionFeeRecord, PaymentTransactionFeeUpdate }
import io.paytouch.core.entities.{ UserContext, PaymentTransactionFee => PaymentTransactionFeeEntity }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class PaymentTransactionFeeService(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends PaymentTransactionFeeConversions {

  type Record = PaymentTransactionFeeRecord
  type Entity = PaymentTransactionFeeEntity

  protected val dao = daos.paymentTransactionFeeDao

  def findByPaymentTransactionIds(paymentTransactionIds: Seq[UUID]): Future[Map[UUID, Seq[Entity]]] =
    dao
      .findByPaymentTransactionIds(paymentTransactionIds)
      .map(_.groupBy(_.paymentTransactionId).transform((_, v) => fromRecordsToEntities(v)))

  def recoverPaymentTransactionFeeUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[PaymentTransactionFeeUpdate]] =
    Future.successful {
      toPaymentTransactionFeeUpdates(upsertion.paymentTransactions)
    }
}
