package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.services._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.features._

class GiftCardPassValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[GiftCardPassRecord]
       with EmailValidator {
  import GiftCardPassService._

  type Record = GiftCardPassRecord
  type Dao = GiftCardPassDao

  protected val dao = daos.giftCardPassDao
  val validationErrorF = InvalidGiftCardPassIds(_)
  val accessErrorF = NonAccessibleGiftCardPassIds(_)

  val giftCardDao = daos.giftCardDao

  def validateBalanceDecrease(
      id: UUID,
      amount: BigDecimal,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Record]]] =
    validateOneById(id).map {
      case Validated.Valid(Some(record)) if amount > record.balanceAmount =>
        Multiple.failure(GiftCardPassNotEnoughBalance(amount, record))

      case Validated.Valid(record) =>
        Multiple.success(record)

      case invalid @ Validated.Invalid(_) =>
        invalid
    }

  def validateBalanceDecrease(
      orderId: OrderId,
      bulkCharge: Seq[GiftCardPassCharge],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Record]]] =
    dao.findAllByMerchantId(user.merchantId).flatMap { records =>
      validateByIds(records.map(_.id)).map {
        _.andThen(doValidateBalanceDecrease(orderId, bulkCharge))
      }
    }

  private def doValidateBalanceDecrease(
      orderId: OrderId,
      bulkCharge: Seq[GiftCardPassCharge],
    )(
      records: Seq[Record],
    ): ErrorsOr[Seq[Record]] = {
    val didNotFindAllRequestedRecords =
      !bulkCharge
        .map(_.giftCardPassId.cast.get.value)
        .toSet
        .subsetOf(records.map(_.id).toSet)

    if (didNotFindAllRequestedRecords)
      Multiple.failure(GiftCardPassesNotAllFound(orderId))
    else {
      val (bad, good) =
        bulkCharge.foldLeft(Seq.empty[GiftCardPassCharge.Failure] -> Seq.empty[Record]) {
          case ((b, g), charge) =>
            val record =
              records
                .find(_.id === charge.giftCardPassId.cast.get.value)
                .get // it's safe to call since all records are found at this point

            if (charge.amount <= record.balanceAmount)
              (b, g :+ record)
            else {
              val failure =
                GiftCardPassCharge
                  .Failure(
                    giftCardPassId = charge.giftCardPassId,
                    requestedAmount = charge.amount,
                    actualBalance = record.balanceAmount,
                  )

              (b :+ failure, g)
            }
        }

      if (bad.isEmpty)
        Multiple.success(good)
      else
        Multiple.failure(InsufficientFunds(orderId, bad))
    }
  }

  def validateCreation(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[GiftCardRecord]] =
    for {
      validGiftCard <- validateGiftCardExists
      validPriceAmounts <- validateItemPriceAmountsExist(upsertion)
    } yield Multiple.combine(validGiftCard, validPriceAmounts) { case (giftCard, _) => giftCard }

  def validateGiftCardExists(implicit user: UserContext): Future[ErrorsOr[GiftCardRecord]] =
    giftCardDao.findOneByMerchantId(user.merchantId).map {
      case Some(giftCard) => Multiple.success(giftCard)
      case None           => Multiple.failure(GiftCardPassWithoutGiftCard())
    }

  private def validateItemPriceAmountsExist(
      upsertion: RecoveredOrderUpsertion,
    ): Future[ErrorsOr[RecoveredOrderUpsertion]] =
    Future {
      val giftCardPassItems = upsertion.items.filter(_.withGiftCardCreation)
      val priceAmounts = giftCardPassItems.map(_.priceAmount)

      if (priceAmounts.forall(_.isDefined))
        Multiple.success(upsertion)
      else {
        val giftCardPassItemsIdsNoPriceAmount =
          giftCardPassItems.filter(_.priceAmount.isEmpty).map(_.id)

        Multiple.failure(GiftCardPassWithoutPrice(giftCardPassItemsIdsNoPriceAmount))
      }
    }

  def canSendReceipt(
      orderItemId: UUID,
      sendReceiptData: SendReceiptData,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[GiftCardPassRecord]] =
    dao.findByOrderItemId(orderItemId).flatMap {
      case Some(result) =>
        for {
          validEmail <- validateEmailFormat(sendReceiptData.recipientEmail)
          giftCardPass <- accessOneById(result.id)
        } yield Multiple.combine(validEmail, giftCardPass) { case (_, gcp) => gcp }

      case None =>
        Future.successful(Multiple.failure(InvalidGiftCardPassOrderItemAssociation(orderItemId)))
    }
}
