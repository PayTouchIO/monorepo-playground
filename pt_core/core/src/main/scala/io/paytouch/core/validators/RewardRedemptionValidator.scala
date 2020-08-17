package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.{ Daos, RewardRedemptionDao }
import io.paytouch.core.data.model.enums.RewardRedemptionType
import io.paytouch.core.data.model.{ LoyaltyMembershipRecord, RewardRedemptionRecord }
import io.paytouch.core.entities.{ OrderUpsertion, RewardRedemptionCreation, RewardRedemptionSync, UserContext }
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class RewardRedemptionValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultValidator[RewardRedemptionRecord] {

  type Record = RewardRedemptionRecord
  type Dao = RewardRedemptionDao

  protected val dao = daos.rewardRedemptionDao
  val validationErrorF = InvalidRewardRedemptionIds(_)
  val accessErrorF = NonAccessibleRewardRedemptionIds(_)

  val loyaltyMembershipValidator = new LoyaltyMembershipValidator
  val loyaltyRewardValidator = new LoyaltyRewardValidator

  def validateCreation(id: UUID, creation: RewardRedemptionCreation)(implicit user: UserContext) =
    for {
      availableId <- availableOneById(id)
      validLoyaltyMembershipId <- membershipExistsAndHasEnoughPoints(creation)
      validLoyaltyReward <- loyaltyRewardValidator.accessOneById(creation.loyaltyRewardId)
    } yield Multiple.combine(availableId, validLoyaltyMembershipId, validLoyaltyReward) {
      case (_, _, loyaltyReward) => loyaltyReward
    }

  private def membershipExistsAndHasEnoughPoints(
      creation: RewardRedemptionCreation,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[LoyaltyMembershipRecord]] =
    loyaltyMembershipValidator.accessOneById(creation.loyaltyMembershipId).map {
      case Valid(lm) if lm.points >= creation.points => Valid(lm)
      case Valid(lm) =>
        Multiple.failure(NotEnoughLoyaltyPoints(currentBalance = lm.points, rewardPoints = creation.points))
      case i @ Invalid(_) => i
    }

  def validateUpsertions(
      orderId: UUID,
      orderUpsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[RewardRedemptionSync]]] = {
    val rewardRedemptionIds = orderUpsertion.rewards.map(_.rewardRedemptionId)
    filterValidByIds(rewardRedemptionIds).map { rewardRedemptions =>
      Multiple.combineSeq(
        orderUpsertion.rewards.map { upsertion =>
          val validRewardRedemptionId =
            recoverRewardRedemptionId(rewardRedemptions, upsertion.rewardRedemptionId, orderId)
          val validRewardRedemptionObjectId = recoverRewardRedemptionObjectId(upsertion, orderUpsertion)
          Multiple.combine(validRewardRedemptionId, validRewardRedemptionObjectId) { case _ => upsertion }
        },
      )
    }
  }

  def recoverUpsertions(
      orderId: UUID,
      orderUpsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[RecoveredRewardRedemptionUpsertion]] = {
    val rewardRedemptionIds = orderUpsertion.rewards.map(_.rewardRedemptionId)
    filterValidByIds(rewardRedemptionIds).map { rewardRedemptions =>
      orderUpsertion.rewards.flatMap { upsertion =>
        val description = s"While validating reward redemption ${upsertion.rewardRedemptionId} for order $orderId"
        val recoveredRewardRedemptionId =
          logger.loggedRecover(recoverRewardRedemptionId(rewardRedemptions, upsertion.rewardRedemptionId, orderId))(
            description,
            upsertion,
          )
        val recoveredRewardRedemptionObjectId =
          logger.loggedRecover(recoverRewardRedemptionObjectId(upsertion, orderUpsertion))(description, upsertion)

        for {
          rewardRedemptionId <- recoveredRewardRedemptionId
          rewardRedemptionObjectId <- recoveredRewardRedemptionObjectId
        } yield toRecoveredRewardRedemptionUpsertion(rewardRedemptionId, rewardRedemptionObjectId, upsertion)
      }
    }
  }

  private def recoverRewardRedemptionId(
      rewardRedemptions: Seq[Record],
      rewardRedemptionId: UUID,
      orderId: UUID,
    ): ErrorsOr[Option[UUID]] =
    rewardRedemptions.find(_.id == rewardRedemptionId) match {
      case Some(rr) if rr.orderId.exists(_ != orderId) =>
        Multiple.failure(RewardRedemptionAlreadyAssociated(rr.id, rr.orderId, orderId))
      case Some(rr) => Multiple.successOpt(rr.id)
      case None     => Multiple.failure(InvalidRewardRedemptionIds(Seq(rewardRedemptionId)))
    }

  private def recoverRewardRedemptionObjectId(
      upsertion: RewardRedemptionSync,
      orderUpsertion: OrderUpsertion,
    ): ErrorsOr[Option[UUID]] =
    upsertion.objectType match {
      case RewardRedemptionType.OrderItem if !orderUpsertion.items.exists(_.id == upsertion.objectId) =>
        Multiple.failure(RewardRedemptionUnmatchedObjectId(upsertion, orderUpsertion.items.map(_.id)))
      case RewardRedemptionType.OrderDiscount if !orderUpsertion.discounts.exists(_.id.contains(upsertion.objectId)) =>
        Multiple.failure(RewardRedemptionUnmatchedObjectId(upsertion, orderUpsertion.discounts.flatMap(_.id)))
      case _ => Multiple.successOpt(upsertion.objectId)
    }

  private def toRecoveredRewardRedemptionUpsertion(
      recoveredRedemptionId: UUID,
      recoveredObjectId: UUID,
      upsertion: RewardRedemptionSync,
    ): RecoveredRewardRedemptionUpsertion =
    RecoveredRewardRedemptionUpsertion(
      rewardRedemptionId = recoveredRedemptionId,
      objectId = Some(recoveredObjectId),
      objectType = upsertion.objectType,
    )

}

final case class RecoveredRewardRedemptionUpsertion(
    rewardRedemptionId: UUID,
    objectId: Option[UUID],
    objectType: RewardRedemptionType,
  )
