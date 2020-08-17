package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class SupplierLocationRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    supplierId: UUID,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickToggleableRecord
       with SlickItemLocationRecord {
  def itemId = supplierId
}

case class SupplierLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    supplierId: Option[UUID],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[SupplierLocationRecord] {
  def toRecord: SupplierLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert SupplierLocationUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert SupplierLocationUpdate without a location id. [$this]")
    require(supplierId.isDefined, s"Impossible to convert SupplierLocationUpdate without a supplier id. [$this]")
    SupplierLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      supplierId = supplierId.get,
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: SupplierLocationRecord): SupplierLocationRecord =
    SupplierLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      supplierId = supplierId.getOrElse(record.supplierId),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
