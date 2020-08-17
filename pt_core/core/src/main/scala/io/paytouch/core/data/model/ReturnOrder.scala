package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ReturnOrderStatus

final case class ReturnOrderRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    supplierId: UUID,
    locationId: UUID,
    purchaseOrderId: Option[UUID],
    number: String,
    notes: Option[String],
    status: ReturnOrderStatus,
    synced: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ReturnOrderUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: Option[UUID],
    supplierId: Option[UUID],
    locationId: Option[UUID],
    purchaseOrderId: Option[UUID],
    notes: Option[String],
    status: Option[ReturnOrderStatus],
    synced: Option[Boolean],
  ) extends SlickMerchantUpdate[ReturnOrderRecord] {

  def toRecord: ReturnOrderRecord = {
    require(merchantId.isDefined, s"Impossible to convert ReturnOrderUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert ReturnOrderUpdate without a user id. [$this]")
    require(supplierId.isDefined, s"Impossible to convert ReturnOrderUpdate without a supplier id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ReturnOrderUpdate without a location id. [$this]")
    require(status.isDefined, s"Impossible to convert ReturnOrderUpdate without a status. [$this]")
    ReturnOrderRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      supplierId = supplierId.get,
      locationId = locationId.get,
      purchaseOrderId = purchaseOrderId,
      number = "",
      notes = notes,
      status = status.get,
      synced = synced.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ReturnOrderRecord): ReturnOrderRecord =
    ReturnOrderRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      supplierId = supplierId.getOrElse(record.supplierId),
      locationId = locationId.getOrElse(record.locationId),
      purchaseOrderId = purchaseOrderId.orElse(record.purchaseOrderId),
      number = record.number,
      notes = notes.orElse(record.notes),
      status = status.getOrElse(record.status),
      synced = synced.getOrElse(record.synced),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
