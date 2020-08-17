package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.KitchenType

final case class KitchenRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    name: String,
    `type`: KitchenType,
    active: Boolean,
    kdsEnabled: Boolean,
    deletedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickSoftDeleteRecord
       with SlickOneToOneWithLocationRecord
case class KitchenUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    name: Option[String],
    `type`: Option[KitchenType],
    active: Option[Boolean],
    kdsEnabled: Option[Boolean],
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[KitchenRecord] {

  def updateRecord(record: KitchenRecord): KitchenRecord =
    KitchenRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      name = name.getOrElse(record.name),
      `type` = `type`.getOrElse(record.`type`),
      active = active.getOrElse(record.active),
      kdsEnabled = kdsEnabled.getOrElse(record.kdsEnabled),
      deletedAt = deletedAt.orElse(record.deletedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: KitchenRecord = {
    require(merchantId.isDefined, s"Impossible to convert KitchenUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert KitchenUpdate without a location id. [$this]")
    require(name.isDefined, s"Impossible to convert KitchenUpdate without a name. [$this]")
    require(`type`.isDefined, s"Impossible to convert KitchenUpdate without a type. [$this]")
    require(active.isDefined, s"Impossible to convert KitchenUpdate without a active flag. [$this]")
    require(kdsEnabled.isDefined, s"Impossible to convert KitchenUpdate without a kds enabled flag. [$this]")
    KitchenRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      name = name.get,
      `type` = `type`.get,
      active = active.get,
      kdsEnabled = kdsEnabled.get,
      deletedAt = deletedAt,
      createdAt = now,
      updatedAt = now,
    )
  }
}
