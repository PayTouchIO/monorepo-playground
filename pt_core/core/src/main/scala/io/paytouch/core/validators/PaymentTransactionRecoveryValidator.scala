package io.paytouch.core.validators

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.core.data.daos.{ Daos, PaymentTransactionDao }
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.PaymentTransactionRecord
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }
import io.paytouch.core.utils.Multiple._

import io.paytouch.core.validators.features.DefaultRecoveryValidator

class PaymentTransactionRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[PaymentTransactionRecord] {
  type Dao = PaymentTransactionDao
  type Record = PaymentTransactionRecord
  type Upsertion = PaymentTransactionUpsertion

  protected val dao = daos.paymentTransactionDao
  val paymentDetailsValidator = new PaymentDetailsRecoveryValidator

  val validationErrorF = InvalidPaymentTransactionIds(_)
  val accessErrorF = NonAccessiblePaymentTransactionIds(_)

  def validateUpsertions(
      orderId: UUID,
      upsertions: Seq[PaymentTransactionUpsertion],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[PaymentTransactionUpsertion]]] = {
    val ids = upsertions.map(_.id)
    val refundedIds = upsertions.flatMap(_.refundedPaymentTransactionId)
    val paymentDetailsPerTransactions =
      upsertions.map(t => t -> t.paymentDetails).toMap
    for {
      availableIds <- filterNonAlreadyTakenIds(ids)
      validPaymentDetailsPerTransaction <-
        paymentDetailsValidator
          .validateUpsertions(
            orderId,
            paymentDetailsPerTransactions,
          )
      validRefundedIds <- filterValidByIds(refundedIds).map(_.map(_.id))
    } yield Multiple.combineSeq(
      upsertions
        .map { paymentTransaction =>
          val validId =
            recoverPaymentTransactionId(availableIds, paymentTransaction)
          val validPaymentDetails: ErrorsOr[Option[PaymentDetails]] =
            validPaymentDetailsPerTransaction
              .getOrElse(paymentTransaction, Multiple.success(None))
          val validRefundedId =
            recoverRefundedPaymentTransactionId(validRefundedIds ++ availableIds, paymentTransaction)
          Multiple.combine(validId, validPaymentDetails, validRefundedId) {
            case _ => paymentTransaction
          }
        },
    )
  }

  def recoverUpsertions(
      orderId: UUID,
      upsertions: Seq[PaymentTransactionUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Seq[RecoveredPaymentTransactionUpsertion]] = {
    val ids = upsertions.map(_.id)
    val refundedIds = upsertions.flatMap(_.refundedPaymentTransactionId)
    val paymentDetailsPerTransactions =
      upsertions.map(t => t -> t.paymentDetails).toMap
    for {
      availableIds <- filterNonAlreadyTakenIds(ids)
      recoveredPaymentDetailsPerTransaction <-
        paymentDetailsValidator
          .recoverUpsertions(
            orderId,
            paymentDetailsPerTransactions,
          )
      validRefundedIds <- filterValidByIds(refundedIds).map(_.map(_.id))
    } yield toRecoveredPaymentTransactions(
      orderId,
      upsertions,
      availableIds,
      validRefundedIds ++ availableIds,
      recoveredPaymentDetailsPerTransaction,
    )
  }

  private def toRecoveredPaymentTransactions(
      orderId: UUID,
      paymentTransactions: Seq[PaymentTransactionUpsertion],
      availablePaymentTransactionIds: Seq[UUID],
      validRefundedIds: Seq[UUID],
      recoveredPaymentDetailsPerTransaction: Map[PaymentTransactionUpsertion, Option[PaymentDetails]],
    )(implicit
      user: UserContext,
    ): Seq[RecoveredPaymentTransactionUpsertion] =
    paymentTransactions
      .map { paymentTransaction =>
        val contextDescription =
          s"While validating payment transaction upsertion of ${paymentTransaction.id} from order $orderId"
        val recoveredId =
          logger.loggedSoftRecoverUUID(recoverPaymentTransactionId(availablePaymentTransactionIds, paymentTransaction))(
            contextDescription,
          )
        val recoveredPaymentDetails =
          recoveredPaymentDetailsPerTransaction.getOrElse(paymentTransaction, None)
        val recoveredRefundedId =
          logger.loggedRecover(recoverRefundedPaymentTransactionId(validRefundedIds, paymentTransaction))(
            contextDescription,
            paymentTransaction,
          )
        val recoveredPaymentProcessor = recoverPaymentProcessor(paymentTransaction, user.paymentProcessor)

        toRecoveredPaymentTransactionUpsertion(
          paymentTransaction,
          recoveredId,
          recoveredRefundedId,
          recoveredPaymentDetails,
          recoveredPaymentProcessor,
        )
      }
      .sortBy(_.refundedPaymentTransactionId)

  private def recoverPaymentTransactionId(
      availablePaymentTransactionIds: Seq[UUID],
      paymentTransaction: Upsertion,
    ): ErrorsOr[UUID] = {
    val id = paymentTransaction.id
    if (availablePaymentTransactionIds.contains(id)) Multiple.success(id)
    else Multiple.failure(NonAccessiblePaymentTransactionIds(Seq(id)))
  }

  private def recoverRefundedPaymentTransactionId(
      availableRefundedIds: Seq[UUID],
      paymentTransaction: Upsertion,
    ): ErrorsOr[Option[UUID]] =
    recoverIdInSeq(
      paymentTransaction.refundedPaymentTransactionId,
      availableRefundedIds,
      NonAccessibleRefundedPaymentTransactionIds.apply,
    )

  private def recoverPaymentProcessor(
      paymentTransactionUpsertion: PaymentTransactionUpsertion,
      merchantPaymentProcessor: PaymentProcessor,
    ): TransactionPaymentProcessor =
    paymentTransactionUpsertion.paymentProcessorV2.getOrElse {
      paymentTransactionUpsertion.paymentType match {
        case Some(TransactionPaymentType.CreditCard) | Some(TransactionPaymentType.DebitCard) =>
          merchantPaymentProcessor.transactionPaymentProcessor
        case Some(TransactionPaymentType.DeliveryProvider) => TransactionPaymentProcessor.DeliveryProvider
        case _                                             => TransactionPaymentProcessor.Paytouch
      }
    }

  private def toRecoveredPaymentTransactionUpsertion(
      paymentTransactionUpsertion: PaymentTransactionUpsertion,
      recoveredId: UUID,
      recoveredRefundedId: Option[UUID],
      recoveredPaymentDetails: Option[PaymentDetails],
      recoveredPaymentProcessor: TransactionPaymentProcessor,
    ): RecoveredPaymentTransactionUpsertion =
    RecoveredPaymentTransactionUpsertion(
      id = recoveredId,
      `type` = paymentTransactionUpsertion.`type`,
      refundedPaymentTransactionId = recoveredRefundedId,
      paymentType = paymentTransactionUpsertion.paymentType,
      paymentDetails = recoveredPaymentDetails,
      version = paymentTransactionUpsertion.version,
      paidAt = paymentTransactionUpsertion.paidAt,
      orderItemIds = paymentTransactionUpsertion.orderItemIds,
      fees = paymentTransactionUpsertion.fees,
      paymentProcessor = recoveredPaymentProcessor,
    )

}

final case class RecoveredPaymentTransactionUpsertion(
    id: UUID,
    `type`: Option[TransactionType],
    refundedPaymentTransactionId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    paymentDetails: Option[PaymentDetails],
    version: Int,
    paidAt: Option[ZonedDateTime],
    orderItemIds: Seq[UUID] = Seq.empty,
    fees: Seq[PaymentTransactionFeeUpsertion],
    paymentProcessor: TransactionPaymentProcessor,
  )
