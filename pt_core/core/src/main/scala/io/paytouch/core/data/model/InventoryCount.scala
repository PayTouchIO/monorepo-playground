package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.InventoryCountStatus

final case class InventoryCountRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    locationId: UUID,
    number: String,
    valueChangeAmount: Option[BigDecimal],
    status: InventoryCountStatus,
    synced: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class InventoryCountUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: Option[UUID],
    locationId: Option[UUID],
    valueChangeAmount: Option[BigDecimal],
    status: Option[InventoryCountStatus],
    synced: Option[Boolean],
  ) extends SlickMerchantUpdate[InventoryCountRecord] {

  def toRecord: InventoryCountRecord = {
    require(merchantId.isDefined, s"Impossible to convert InventoryCountUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert InventoryCountUpdate without a user id. [$this]")
    require(locationId.isDefined, s"Impossible to convert InventoryCountUpdate without a location id. [$this]")
    InventoryCountRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      locationId = locationId.get,
      number = "", // overridden in upsertion query
      valueChangeAmount = valueChangeAmount,
      status = status.getOrElse(InventoryCountStatus.InProgress),
      synced = synced.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: InventoryCountRecord): InventoryCountRecord =
    InventoryCountRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      locationId = locationId.getOrElse(record.locationId),
      number = record.number,
      valueChangeAmount = valueChangeAmount.orElse(record.valueChangeAmount),
      status = status.getOrElse(record.status),
      synced = synced.getOrElse(record.synced),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
