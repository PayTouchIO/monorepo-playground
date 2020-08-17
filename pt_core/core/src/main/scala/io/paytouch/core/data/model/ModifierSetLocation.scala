package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ModifierSetLocationRecord(
    id: UUID,
    merchantId: UUID,
    modifierSetId: UUID,
    locationId: UUID,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickToggleableRecord
       with SlickItemLocationRecord {
  def itemId = modifierSetId
}

case class ModifierSetLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    modifierSetId: Option[UUID],
    locationId: Option[UUID],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[ModifierSetLocationRecord] {

  def toRecord: ModifierSetLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert ModifierSetLocationUpdate without a merchant id. [$this]")
    require(
      modifierSetId.isDefined,
      s"Impossible to convert ModifierSetLocationUpdate without a modifier set id. [$this]",
    )
    require(locationId.isDefined, s"Impossible to convert ModifierSetLocationUpdate without a location id. [$this]")
    ModifierSetLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      modifierSetId = modifierSetId.get,
      locationId = locationId.get,
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )

  }

  def updateRecord(record: ModifierSetLocationRecord): ModifierSetLocationRecord =
    ModifierSetLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      modifierSetId = modifierSetId.getOrElse(record.modifierSetId),
      locationId = locationId.getOrElse(record.locationId),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
