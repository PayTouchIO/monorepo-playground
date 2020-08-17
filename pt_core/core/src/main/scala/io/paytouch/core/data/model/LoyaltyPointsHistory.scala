package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ LoyaltyPointsHistoryRelatedType, LoyaltyPointsHistoryType }

final case class LoyaltyPointsHistoryRecord(
    id: UUID,
    merchantId: UUID,
    loyaltyMembershipId: UUID,
    `type`: LoyaltyPointsHistoryType,
    points: Int,
    orderId: Option[UUID],
    objectId: Option[UUID],
    objectType: Option[LoyaltyPointsHistoryRelatedType],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class LoyaltyPointsHistoryUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    loyaltyMembershipId: Option[UUID],
    `type`: Option[LoyaltyPointsHistoryType],
    points: Option[Int],
    orderId: Option[UUID],
    objectId: Option[UUID],
    objectType: Option[LoyaltyPointsHistoryRelatedType],
  ) extends SlickMerchantUpdate[LoyaltyPointsHistoryRecord] {

  def toRecord: LoyaltyPointsHistoryRecord = {
    require(merchantId.isDefined, s"Impossible to convert LoyaltyPointsHistoryUpdate without a merchant id. [$this]")
    require(
      loyaltyMembershipId.isDefined,
      s"Impossible to convert LoyaltyPointsHistoryUpdate without a loyalty status id. [$this]",
    )
    require(
      `type`.isDefined,
      s"Impossible to convert LoyaltyPointsHistoryUpdate without loyalty point history type. [$this]",
    )
    require(points.isDefined, s"Impossible to convert LoyaltyPointsHistoryUpdate without points. [$this]")
    LoyaltyPointsHistoryRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      loyaltyMembershipId = loyaltyMembershipId.get,
      `type` = `type`.get,
      points = points.get,
      orderId = orderId,
      objectId = objectId,
      objectType = objectType,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LoyaltyPointsHistoryRecord): LoyaltyPointsHistoryRecord =
    LoyaltyPointsHistoryRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      loyaltyMembershipId = loyaltyMembershipId.getOrElse(record.loyaltyMembershipId),
      `type` = `type`.getOrElse(record.`type`),
      points = points.getOrElse(record.points),
      orderId = orderId.orElse(record.orderId),
      objectId = objectId.orElse(record.objectId),
      objectType = objectType.orElse(record.objectType),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
