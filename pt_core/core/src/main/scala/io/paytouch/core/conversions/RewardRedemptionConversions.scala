package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.enums.{ LoyaltyPointsHistoryType, RewardRedemptionStatus }
import io.paytouch.core.data.model.{
  LoyaltyPointsHistoryUpdate,
  LoyaltyRewardRecord,
  RewardRedemptionRecord,
  RewardRedemptionUpdate => RewardRedemptionUpdateModel,
}
import io.paytouch.core.entities.{
  LoyaltyMembership,
  LoyaltyReward,
  MerchantContext,
  RewardRedemptionCreation,
  UserContext,
  RewardRedemption => RewardRedemptionEntity,
}
import io.paytouch.core.validators.RecoveredRewardRedemptionUpsertion

trait RewardRedemptionConversions
    extends EntityConversionMerchantContext[RewardRedemptionRecord, RewardRedemptionEntity]
       with LoyaltyPointsHistoryConversions {

  def fromRecordToEntity(record: RewardRedemptionRecord)(implicit merchant: MerchantContext): RewardRedemptionEntity =
    fromRecordToEntity(record, None, None)

  def fromRecordsToEntities(
      records: Seq[RewardRedemptionRecord],
      loyaltyMembershipsPerRewardRedemption: Option[Map[RewardRedemptionRecord, LoyaltyMembership]],
      loyaltyRewardsPerRewardRedemption: Map[RewardRedemptionRecord, LoyaltyReward],
    )(implicit
      merchant: MerchantContext,
    ): Seq[RewardRedemptionEntity] =
    records.map { record =>
      val loyaltyMembership = loyaltyMembershipsPerRewardRedemption.flatMap(_.get(record))
      val loyaltyReward = loyaltyRewardsPerRewardRedemption.get(record)
      fromRecordToEntity(record, loyaltyMembership, loyaltyReward)
    }

  def fromRecordToEntity(
      record: RewardRedemptionRecord,
      loyaltyMembership: Option[LoyaltyMembership],
      loyaltyReward: Option[LoyaltyReward],
    )(implicit
      merchant: MerchantContext,
    ): RewardRedemptionEntity =
    RewardRedemptionEntity(
      id = record.id,
      loyaltyRewardId = record.loyaltyRewardId,
      loyaltyMembershipId = record.loyaltyMembershipId,
      points = record.points,
      status = record.status,
      loyaltyMembership = loyaltyMembership,
      loyaltyRewardType = record.loyaltyRewardType,
      orderId = record.orderId,
      objectId = record.objectId,
      objectType = record.objectType,
    )

  def toRedeemedHistoryUpdate(
      rewardRedemption: RewardRedemptionRecord,
    )(implicit
      user: UserContext,
    ): LoyaltyPointsHistoryUpdate =
    toLoyaltyPointHistoryUpdate(
      loyaltyMembershipId = rewardRedemption.loyaltyMembershipId,
      merchantId = user.merchantId,
      orderId = rewardRedemption.orderId,
      objectId = rewardRedemption.id,
      `type` = LoyaltyPointsHistoryType.RewardRedemption,
      points = -rewardRedemption.points,
    )

  def fromCreationToUpdate(
      id: UUID,
      creation: RewardRedemptionCreation,
      loyaltyReward: LoyaltyRewardRecord,
    )(implicit
      user: UserContext,
    ): RewardRedemptionUpdateModel =
    RewardRedemptionUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      loyaltyRewardId = Some(creation.loyaltyRewardId),
      loyaltyRewardType = Some(loyaltyReward.`type`),
      loyaltyMembershipId = Some(creation.loyaltyMembershipId),
      points = Some(creation.points),
      status = Some(RewardRedemptionStatus.Reserved),
      orderId = None,
      objectId = None,
      objectType = None,
    )

  def fromRecoveredUpsertionToUpdate(orderId: UUID, rewardRedemption: RecoveredRewardRedemptionUpsertion) =
    RewardRedemptionUpdateModel(
      id = Some(rewardRedemption.rewardRedemptionId),
      merchantId = None,
      loyaltyRewardId = None,
      loyaltyRewardType = None,
      loyaltyMembershipId = None,
      points = None,
      status = Some(RewardRedemptionStatus.Redeemed),
      orderId = Some(orderId),
      objectId = rewardRedemption.objectId,
      objectType = Some(rewardRedemption.objectType),
    )

  def toCancelHistoryUpdate(rewardRedemption: RewardRedemptionRecord)(implicit user: UserContext) =
    toLoyaltyPointHistoryUpdate(
      loyaltyMembershipId = rewardRedemption.loyaltyMembershipId,
      merchantId = user.merchantId,
      objectId = rewardRedemption.id,
      orderId = rewardRedemption.orderId,
      `type` = LoyaltyPointsHistoryType.RewardCancel,
      points = rewardRedemption.points,
    )

  def toCancelUpdate(id: UUID) =
    RewardRedemptionUpdateModel(
      id = Some(id),
      merchantId = None,
      loyaltyRewardId = None,
      loyaltyRewardType = None,
      loyaltyMembershipId = None,
      points = None,
      status = Some(RewardRedemptionStatus.Canceled),
      orderId = None,
      objectId = None,
      objectType = None,
    )
}
