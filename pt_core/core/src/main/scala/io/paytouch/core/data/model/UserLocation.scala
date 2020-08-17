package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class UserLocationRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    userId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class UserLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    userId: Option[UUID],
  ) extends SlickMerchantUpdate[UserLocationRecord] {
  def toRecord: UserLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert UsermerchantUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert UserLocationUpdate without a location id. [$this]")
    require(userId.isDefined, s"Impossible to convert UserLocationUpdate without a user id. [$this]")
    UserLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      userId = userId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: UserLocationRecord): UserLocationRecord =
    UserLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
