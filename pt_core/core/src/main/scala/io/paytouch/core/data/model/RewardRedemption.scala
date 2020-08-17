package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ RewardRedemptionStatus, RewardRedemptionType }
import io.paytouch.core.entities.enums.RewardType

final case class RewardRedemptionRecord(
    id: UUID,
    merchantId: UUID,
    loyaltyRewardId: UUID,
    loyaltyRewardType: RewardType,
    loyaltyMembershipId: UUID,
    points: Int,
    status: RewardRedemptionStatus,
    orderId: Option[UUID],
    objectId: Option[UUID],
    objectType: Option[RewardRedemptionType],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class RewardRedemptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    loyaltyRewardId: Option[UUID],
    loyaltyRewardType: Option[RewardType],
    loyaltyMembershipId: Option[UUID],
    points: Option[Int],
    status: Option[RewardRedemptionStatus],
    orderId: Option[UUID],
    objectId: Option[UUID],
    objectType: Option[RewardRedemptionType],
  ) extends SlickMerchantUpdate[RewardRedemptionRecord] {

  def toRecord: RewardRedemptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert RewardRedemptionUpdate without a merchant id. [$this]")
    require(
      loyaltyRewardId.isDefined,
      s"Impossible to convert RewardRedemptionUpdate without a loyalty reward id. [$this]",
    )
    require(
      loyaltyRewardType.isDefined,
      s"Impossible to convert RewardRedemptionUpdate without a loyalty reward type. [$this]",
    )
    require(
      loyaltyMembershipId.isDefined,
      s"Impossible to convert RewardRedemptionUpdate without a loyalty membership id. [$this]",
    )
    require(points.isDefined, s"Impossible to convert RewardRedemptionUpdate without points. [$this]")
    require(status.isDefined, s"Impossible to convert RewardRedemptionUpdate without a status. [$this]")
    RewardRedemptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      loyaltyRewardId = loyaltyRewardId.get,
      loyaltyRewardType = loyaltyRewardType.get,
      loyaltyMembershipId = loyaltyMembershipId.get,
      points = points.get,
      status = status.get,
      orderId = orderId,
      objectId = objectId,
      objectType = objectType,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: RewardRedemptionRecord): RewardRedemptionRecord =
    RewardRedemptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      loyaltyRewardId = loyaltyRewardId.getOrElse(record.loyaltyRewardId),
      loyaltyRewardType = loyaltyRewardType.getOrElse(record.loyaltyRewardType),
      loyaltyMembershipId = loyaltyMembershipId.getOrElse(record.loyaltyMembershipId),
      points = points.getOrElse(record.points),
      status = status.getOrElse(record.status),
      orderId = orderId.orElse(record.orderId),
      objectId = objectId.orElse(record.objectId),
      objectType = objectType.orElse(record.objectType),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
