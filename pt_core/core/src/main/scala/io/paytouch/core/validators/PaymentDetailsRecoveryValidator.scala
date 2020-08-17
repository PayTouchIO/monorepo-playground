package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import io.paytouch.core.utils.Multiple._

import scala.concurrent._

class PaymentDetailsRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends RecoveryValidatorUtils {

  type Record = PaymentDetails

  val passValidator = new GiftCardPassValidator
  val passTransactionValidator = new GiftCardPassTransactionValidator

  def validateUpsertions(
      orderId: UUID,
      upsertions: Map[PaymentTransactionUpsertion, Option[Record]],
    )(implicit
      user: UserContext,
    ): Future[Map[PaymentTransactionUpsertion, ErrorsOr[Option[Record]]]] = {
    val paymentDetails = upsertions.values.flatten.toSeq
    val giftCardPassIds = paymentDetails.flatMap(_.giftCardPassId)
    val giftCardPassTransactionIds = paymentDetails.flatMap(_.giftCardPassTransactionId)
    for {
      validPasses <- passValidator.filterValidByIds(giftCardPassIds)
      validPassIds = validPasses.map(_.id)
      validPassTransactions <- passTransactionValidator.filterValidByIds(giftCardPassTransactionIds)
      validPassTransactionIds = validPassTransactions.map(_.id)
    } yield upsertions.map {
      case (transaction, maybePaymentDetails) =>
        val transactionId = transaction.id
        val validatedPaymentDetails = maybePaymentDetails match {
          case None => Multiple.success(None)
          case Some(paymentDetails) =>
            val validGiftCardPassId = recoverGiftCardPassId(paymentDetails, validPassIds)
            val validGiftCardPassTransactionId =
              recoverGiftCardPassTransactionId(paymentDetails, validPassTransactionIds)
            Multiple.combine(validGiftCardPassId, validGiftCardPassTransactionId) {
              case _ => Some(paymentDetails)
            }
        }
        transaction -> validatedPaymentDetails
    }
  }

  def recoverUpsertions(
      orderId: UUID,
      upsertions: Map[PaymentTransactionUpsertion, Option[Record]],
    )(implicit
      user: UserContext,
    ): Future[Map[PaymentTransactionUpsertion, Option[Record]]] = {
    val paymentDetails = upsertions.values.flatten.toSeq
    val giftCardPassIds = paymentDetails.flatMap(_.giftCardPassId)
    val giftCardPassTransactionIds = paymentDetails.flatMap(_.giftCardPassTransactionId)
    for {
      validPasses <- passValidator.filterValidByIds(giftCardPassIds)
      validPassIds = validPasses.map(_.id)
      validPassTransactions <- passTransactionValidator.filterValidByIds(giftCardPassTransactionIds)
      validPassTransactionIds = validPassTransactions.map(_.id)
    } yield toRecoveredPaymentDetailsPerTransactions(orderId, upsertions, validPassIds, validPassTransactionIds)
  }

  private def toRecoveredPaymentDetailsPerTransactions(
      orderId: UUID,
      upsertions: Map[PaymentTransactionUpsertion, Option[Record]],
      validPassIds: Seq[UUID],
      validPassTransactionIds: Seq[UUID],
    ): Map[PaymentTransactionUpsertion, Option[Record]] =
    upsertions.map {
      case (transaction, maybePaymentDetails) =>
        val transactionId = transaction.id
        val recoveredPaymentDetails = maybePaymentDetails.map(pd =>
          toRecoveredPaymentDetails(orderId, transactionId, pd, validPassIds, validPassTransactionIds),
        )
        transaction -> recoveredPaymentDetails
    }

  private def toRecoveredPaymentDetails(
      orderId: UUID,
      transactionId: UUID,
      paymentDetails: Record,
      validPassIds: Seq[UUID],
      validPassTransactionIds: Seq[UUID],
    ): Record = {
    def loggedRecover(recoveredData: ErrorsOr[Option[UUID]]): Option[UUID] = {
      val ctx = s"While validating payment details of transaction $transactionId from order $orderId"
      logger.loggedRecover(recoveredData)(ctx, paymentDetails)
    }
    val recoveredGiftCardPassId = loggedRecover(recoverGiftCardPassId(paymentDetails, validPassIds))
    val recoveredGiftCardPassTransactionId = loggedRecover(
      recoverGiftCardPassTransactionId(paymentDetails, validPassTransactionIds),
    )
    paymentDetails.copy(
      giftCardPassId = recoveredGiftCardPassId,
      giftCardPassTransactionId = recoveredGiftCardPassTransactionId,
    )
  }

  private def recoverGiftCardPassId(paymentDetails: Record, validPassIds: Seq[UUID]): ErrorsOr[Option[UUID]] =
    recoverIdInSeq(paymentDetails.giftCardPassId, validPassIds, NonAccessibleGiftCardPassIds.apply)

  private def recoverGiftCardPassTransactionId(
      paymentDetails: Record,
      validPassTransactionIds: Seq[UUID],
    ): ErrorsOr[Option[UUID]] =
    recoverIdInSeq(
      paymentDetails.giftCardPassTransactionId,
      validPassTransactionIds,
      NonAccessibleGiftCardPassTransactionIds.apply,
    )
}
