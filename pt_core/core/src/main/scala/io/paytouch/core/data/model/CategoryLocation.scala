package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class CategoryLocationRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    categoryId: UUID,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickToggleableRecord
       with SlickItemLocationRecord {
  def itemId = categoryId
}

case class CategoryLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    categoryId: Option[UUID],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[CategoryLocationRecord] {

  def toRecord: CategoryLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert CategoryLocationUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert CategoryLocationUpdate without a location id. [$this]")
    require(categoryId.isDefined, s"Impossible to convert CategoryLocationUpdate without a category id. [$this]")
    CategoryLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      categoryId = categoryId.get,
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CategoryLocationRecord): CategoryLocationRecord =
    CategoryLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      categoryId = categoryId.getOrElse(record.categoryId),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
