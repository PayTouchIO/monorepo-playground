package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class DiscountLocationRecord(
    id: UUID,
    merchantId: UUID,
    discountId: UUID,
    locationId: UUID,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickToggleableRecord
       with SlickItemLocationRecord {
  def itemId = discountId
}

case class DiscountLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    discountId: Option[UUID],
    locationId: Option[UUID],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[DiscountLocationRecord] {

  def toRecord: DiscountLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert DiscountLocationUpdate without a merchant id. [$this]")
    require(discountId.isDefined, s"Impossible to convert DiscountLocationUpdate without a discount id. [$this]")
    require(locationId.isDefined, s"Impossible to convert DiscountLocationUpdate without a location id. [$this]")
    DiscountLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      discountId = discountId.get,
      locationId = locationId.get,
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: DiscountLocationRecord): DiscountLocationRecord =
    DiscountLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      discountId = discountId.getOrElse(record.discountId),
      locationId = locationId.getOrElse(record.locationId),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
