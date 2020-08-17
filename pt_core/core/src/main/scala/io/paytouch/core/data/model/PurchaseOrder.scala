package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ PurchaseOrderPaymentStatus, ReceivingObjectStatus }

final case class PurchaseOrderRecord(
    id: UUID,
    merchantId: UUID,
    supplierId: UUID,
    locationId: UUID,
    userId: UUID,
    number: String,
    sent: Boolean,
    paymentStatus: Option[PurchaseOrderPaymentStatus],
    expectedDeliveryDate: Option[ZonedDateTime],
    status: ReceivingObjectStatus,
    notes: Option[String],
    deletedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickSoftDeleteRecord
       with SlickReceivingObjectRecord

case class PurchaseOrderUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    supplierId: Option[UUID],
    locationId: Option[UUID],
    userId: Option[UUID],
    paymentStatus: Option[PurchaseOrderPaymentStatus],
    expectedDeliveryDate: Option[ZonedDateTime],
    status: Option[ReceivingObjectStatus] = None,
    sent: Option[Boolean],
    notes: Option[String],
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[PurchaseOrderRecord] {

  def toRecord: PurchaseOrderRecord = {
    require(merchantId.isDefined, s"Impossible to convert PurchaseOrderUpdate without a merchant id. [$this]")
    require(supplierId.isDefined, s"Impossible to convert PurchaseOrderUpdate without a supplier id. [$this]")
    require(locationId.isDefined, s"Impossible to convert PurchaseOrderUpdate without a location id. [$this]")
    require(userId.isDefined, s"Impossible to convert PurchaseOrderUpdate without a user id. [$this]")
    PurchaseOrderRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      supplierId = supplierId.get,
      locationId = locationId.get,
      userId = userId.get,
      number = "", // overridden in upsertion query
      sent = sent.getOrElse(false),
      paymentStatus = paymentStatus,
      expectedDeliveryDate = expectedDeliveryDate,
      status = status.getOrElse(ReceivingObjectStatus.Created),
      notes = notes,
      deletedAt = deletedAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PurchaseOrderRecord): PurchaseOrderRecord =
    PurchaseOrderRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      supplierId = supplierId.getOrElse(record.supplierId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      number = record.number,
      sent = sent.getOrElse(record.sent),
      paymentStatus = paymentStatus.orElse(record.paymentStatus),
      expectedDeliveryDate = expectedDeliveryDate.orElse(record.expectedDeliveryDate),
      status = status.getOrElse(record.status),
      notes = notes.orElse(record.notes),
      deletedAt = deletedAt.orElse(record.deletedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
