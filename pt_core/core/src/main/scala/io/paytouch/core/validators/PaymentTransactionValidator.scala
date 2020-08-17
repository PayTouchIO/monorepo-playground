package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.Valid
import io.paytouch.core.data.daos.{ Daos, PaymentTransactionDao }
import io.paytouch.core.data.model.PaymentTransactionRecord
import io.paytouch.core.data.model.enums.{ PaymentProcessor, TransactionPaymentProcessor, TransactionType }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class PaymentTransactionValidator(val orderValidator: OrderValidator)(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[PaymentTransactionRecord] {

  type Dao = PaymentTransactionDao
  type Record = PaymentTransactionRecord

  protected val dao = daos.paymentTransactionDao
  val validationErrorF = InvalidPaymentTransactionIds(_)
  val accessErrorF = NonAccessiblePaymentTransactionIds(_)

  def validateUpsertion(
      orderId: UUID,
      paymentTransactions: Seq[RecoveredPaymentTransactionUpsertion],
    )(implicit
      user: UserContext,
    ) =
    for {
      validPaymentTransactions <- validateByIds(paymentTransactions.map(_.id))
      validOrderId <- orderValidator.validateOneById(orderId)
    } yield Multiple.combine(validPaymentTransactions, validOrderId) {
      case (paymentTransacts, _) => paymentTransacts
    }

  def accessOneByIdAndOrderId(id: UUID, orderId: UUID)(implicit user: UserContext): Future[ErrorsOr[Record]] =
    accessOneById(id).map {
      case Valid(paymentTransaction) if paymentTransaction.orderId != orderId =>
        Multiple.failure(InvalidPaymentTransactionIdForOrderId(paymentTransaction.id, orderId))
      case x => x
    }

  def validateRefund(
      paymentTransactionId: UUID,
      paymentProcessor: TransactionPaymentProcessor,
      amount: Option[BigDecimal],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Record]] =
    for {
      paymentTransaction <- accessOneById(paymentTransactionId)
      validPaymentProcessor <- validatePaymentProcessor(paymentTransaction.toOption, paymentProcessor)
      validTransactionType <- validateTransactionType(paymentTransaction.toOption)
      validRefundAmount <- validateRefundAmount(paymentTransaction.toOption, amount)
    } yield Multiple.combine(paymentTransaction, validPaymentProcessor, validTransactionType, validRefundAmount) {
      case (transaction, _, _, _) => transaction
    }

  private def validatePaymentProcessor(
      transaction: Option[Record],
      paymentProcessor: TransactionPaymentProcessor,
    ): Future[ErrorsOr[TransactionPaymentProcessor]] =
    Future.successful {
      transaction match {
        case Some(tx) if tx.paymentProcessor != paymentProcessor =>
          Multiple.failure(PaymentTransactionProcessorMismatch(paymentProcessor, tx.paymentProcessor))
        case _ =>
          Multiple.success(paymentProcessor)
      }
    }

  private def validateTransactionType(transaction: Option[Record]): Future[ErrorsOr[TransactionType]] =
    Future.successful {
      val expected = TransactionType.Payment
      transaction match {
        case Some(tx) if !tx.`type`.contains(expected) =>
          Multiple.failure(PaymentTransactionTypeMismatch(expected, tx.`type`))
        case _ =>
          Multiple.success(expected)
      }
    }

  private def validateRefundAmount(
      transaction: Option[Record],
      amount: Option[BigDecimal],
    ): Future[ErrorsOr[Option[BigDecimal]]] =
    Future.successful {
      val transactionAmount: Option[BigDecimal] = transaction.flatMap(_.paymentDetails).flatMap(_.amount)

      (transactionAmount, amount) match {
        case (Some(ta), Some(am)) if am <= ta =>
          Multiple.success(amount)
        case (Some(ta), Some(am)) =>
          Multiple.failure(InvalidRefundAmount(am, ta))
        case _ =>
          Multiple.empty
      }
    }
}
