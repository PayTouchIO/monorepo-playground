package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class LoyaltyProgramLocationRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    loyaltyProgramId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickItemLocationRecord {
  def itemId = loyaltyProgramId
}

case class LoyaltyProgramLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    loyaltyProgramId: Option[UUID],
  ) extends SlickMerchantUpdate[LoyaltyProgramLocationRecord] {

  def toRecord: LoyaltyProgramLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert LoyaltyProgramLocationUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert LoyaltyProgramLocationUpdate without a location id. [$this]")
    require(
      loyaltyProgramId.isDefined,
      s"Impossible to convert LoyaltyProgramLocationUpdate without a loyalty program id. [$this]",
    )
    LoyaltyProgramLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      loyaltyProgramId = loyaltyProgramId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LoyaltyProgramLocationRecord): LoyaltyProgramLocationRecord =
    LoyaltyProgramLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      loyaltyProgramId = loyaltyProgramId.getOrElse(record.loyaltyProgramId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
