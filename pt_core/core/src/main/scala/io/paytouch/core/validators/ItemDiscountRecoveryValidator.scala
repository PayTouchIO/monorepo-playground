package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.model.{ DiscountRecord, SlickMerchantRecord }
import io.paytouch.core.data.model.enums.DiscountType
import io.paytouch.core.entities.{ ItemDiscountUpsertion, UserContext }
import io.paytouch.core.errors.{ InvalidDiscountIds, NonAccessibleIds, ZeroDiscount }
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

trait ItemDiscountRecoveryValidator[Record <: SlickMerchantRecord]
    extends DefaultValidator[Record]
       with RecoveryValidatorUtils {

  implicit def logger: PaytouchLogger

  def errorMsg: Seq[UUID] => NonAccessibleIds

  val discountValidator = new DiscountValidator

  protected def validIdsInUpsertion[T](
      itemIds: Seq[UUID],
      discountIds: Seq[UUID],
    )(
      f: (Seq[UUID], Seq[DiscountRecord]) => T,
    )(implicit
      user: UserContext,
    ): Future[T] =
    for {
      validItemDiscountIds <- filterNonAlreadyTakenIds(itemIds)
      discounts <- discountValidator.filterValidByIds(discountIds)
    } yield f(validItemDiscountIds, discounts)

  protected def validateItemDiscountUpsertion(
      upsertions: Seq[ItemDiscountUpsertion],
      validItemDiscountIds: Seq[UUID],
      discounts: Seq[DiscountRecord],
    ): ErrorsOr[Seq[ItemDiscountUpsertion]] =
    Multiple.combineSeq(upsertions.map { upsertion =>
      val validNotZeroDiscount = filterZeroDiscount(upsertion)
      val validId = recoverIdInSeq(upsertion.id, validItemDiscountIds, errorMsg)
      val validDiscountId = recoverDiscountId(discounts, upsertion.discountId)
      Multiple.combine(validNotZeroDiscount, validId, validDiscountId) { case _ => upsertion }
    })

  protected def toRecoveredItemDiscountUpsertion(
      upsertions: Seq[ItemDiscountUpsertion],
      validItemDiscountIds: Seq[UUID],
      discounts: Seq[DiscountRecord],
    ): Seq[RecoveredItemDiscountUpsertion] =
    upsertions.flatMap { upsertion =>
      val filteredZeroDiscount = logger.loggedSoftRecover(filterZeroDiscount(upsertion))("Filtered out zero discount")
      filteredZeroDiscount.map { _ =>
        val recoveredId = logger.loggedSoftRecover(recoverIdInSeq(upsertion.id, validItemDiscountIds, errorMsg))(
          "Item discount id already taken, generating a new one",
        )
        val recoveredDiscountId = logger.loggedSoftRecover(recoverDiscountId(discounts, upsertion.discountId))(
          "Discount not found, assuming it has been deleted",
        )
        RecoveredItemDiscountUpsertion(
          id = recoveredId,
          discountId = recoveredDiscountId,
          title = upsertion.title,
          `type` = upsertion.`type`,
          amount = upsertion.amount,
          totalAmount = upsertion.totalAmount,
        )
      }
    }

  private def recoverDiscountId(discounts: Seq[DiscountRecord], discountId: Option[UUID]): ErrorsOr[Option[UUID]] =
    discountId match {
      case Some(discId) if discounts.exists(_.id == discId) => Multiple.successOpt(discId)
      case Some(discId)                                     => Multiple.failure(InvalidDiscountIds(Seq(discId)))
      case None                                             => Multiple.empty
    }

  private def filterZeroDiscount(upsertion: ItemDiscountUpsertion): ErrorsOr[Option[ItemDiscountUpsertion]] =
    upsertion match {
      case u if u.discountId.isEmpty && u.amount == 0 && u.title.isEmpty =>
        Multiple.failure(ZeroDiscount(upsertion))
      case u => Multiple.successOpt(u)
    }
}

final case class RecoveredItemDiscountUpsertion(
    id: Option[UUID],
    discountId: Option[UUID],
    title: Option[String],
    `type`: DiscountType,
    amount: BigDecimal,
    totalAmount: Option[BigDecimal],
  )
