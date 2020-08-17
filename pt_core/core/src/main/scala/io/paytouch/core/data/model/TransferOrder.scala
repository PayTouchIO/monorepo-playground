package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, TransferOrderType }

final case class TransferOrderRecord(
    id: UUID,
    merchantId: UUID,
    fromLocationId: UUID,
    toLocationId: UUID,
    userId: UUID,
    number: String,
    notes: Option[String],
    status: ReceivingObjectStatus,
    `type`: TransferOrderType,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickReceivingObjectRecord

case class TransferOrderUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    fromLocationId: Option[UUID],
    toLocationId: Option[UUID],
    userId: Option[UUID],
    notes: Option[String],
    status: Option[ReceivingObjectStatus] = None,
    `type`: Option[TransferOrderType],
  ) extends SlickMerchantUpdate[TransferOrderRecord] {

  def toRecord: TransferOrderRecord = {
    require(merchantId.isDefined, s"Impossible to convert TransferOrderUpdate without a merchant id. [$this]")
    require(fromLocationId.isDefined, s"Impossible to convert TransferOrderUpdate without a from location id. [$this]")
    require(toLocationId.isDefined, s"Impossible to convert TransferOrderUpdate without a to location id. [$this]")
    require(userId.isDefined, s"Impossible to convert TransferOrderUpdate without a user id. [$this]")
    require(`type`.isDefined, s"Impossible to convert TransferOrderUpdate without a `type`. [$this]")
    TransferOrderRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      fromLocationId = fromLocationId.get,
      toLocationId = toLocationId.get,
      userId = userId.get,
      number = "",
      notes = notes,
      status = status.getOrElse(ReceivingObjectStatus.Created),
      `type` = `type`.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TransferOrderRecord): TransferOrderRecord =
    TransferOrderRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      fromLocationId = fromLocationId.getOrElse(record.fromLocationId),
      toLocationId = toLocationId.getOrElse(record.toLocationId),
      userId = userId.getOrElse(record.userId),
      number = record.number,
      notes = notes.orElse(record.notes),
      status = status.getOrElse(record.status),
      `type` = `type`.getOrElse(record.`type`),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
