package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.conversions.PaymentTransactionConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ PaymentTransactionRecord, PaymentTransactionUpdate }
import io.paytouch.core.entities.{ UserContext, PaymentTransaction => PaymentTransactionEntity }
import io.paytouch.core.errors.PaymentTransactionExpired
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.{ Multiple, PaytouchLogger, UtcTime }
import io.paytouch.core.validators.{ OrderValidator, RecoveredOrderUpsertion }

import scala.concurrent._

class PaymentTransactionService(
    giftCardPassService: => GiftCardPassService,
    paymentTransactionFeeService: PaymentTransactionFeeService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends PaymentTransactionConversions {

  type Record = PaymentTransactionRecord
  type Entity = PaymentTransactionEntity

  protected val dao = daos.paymentTransactionDao
  protected val validator = (new OrderValidator).paymentTransactionValidator
  val paymentTransactionOrderItemDao = daos.paymentTransactionOrderItemDao

  def findRecordsById(orderId: UUID): Future[Seq[Record]] = dao.findByOrderIds(Seq(orderId))

  def findByOrderIds(orderIds: Seq[UUID]): Future[Seq[Entity]] =
    for {
      paymentTransactions <- dao.findByOrderIds(orderIds)
      paymentTransactionIds = paymentTransactions.map(_.id)
      lookupIdPerGiftCardId <- getLookupIdPerGiftCardPassId(paymentTransactions)
      orderItemsIds <- paymentTransactionOrderItemDao.findByPaymentTransactionIds(paymentTransactionIds)
      fees <- paymentTransactionFeeService.findByPaymentTransactionIds(paymentTransactionIds)
    } yield fromRecordsAndOptionsToEntities(paymentTransactions, lookupIdPerGiftCardId, orderItemsIds, fees)

  def getLookupIdPerGiftCardPassId(records: Seq[Record]): Future[Map[UUID, String]] = {
    val giftCardPassIds = giftCardIdForTransactions(records)
    giftCardPassService.findRecordsByIds(giftCardPassIds).map {
      _.map(giftCardPass => giftCardPass.id -> giftCardPass.lookupId).toMap
    }
  }

  def recoverPaymentTransactionUpdates(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[PaymentTransactionUpdate]] = {
    val orderId = upsertion.orderId
    val paymentTransactions = upsertion.paymentTransactions
    validator
      .validateUpsertion(orderId, paymentTransactions)
      .mapNested(_ => toPaymentTransactionUpdates(upsertion))
      .map { validation =>
        val description = "While converting payment transactions from recovered order upsertion"
        logger.loggedRecoverSeq(validation)(description, upsertion)
      }
  }

  def validate(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    validator.accessOneById(id).map {
      case Valid(paymentTransaction) if isValid(paymentTransaction) => Multiple.success((): Unit)
      case i @ Invalid(_)                                           => i
      case _                                                        => Multiple.failure(PaymentTransactionExpired(id))
    }

  private def isValid(record: Record): Boolean = record.paidAt.exists(_.plusDays(2).isAfter(UtcTime.now))
}
