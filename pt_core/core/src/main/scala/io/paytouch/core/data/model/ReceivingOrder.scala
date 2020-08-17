package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{
  ReceivingOrderObjectType,
  ReceivingOrderPaymentMethod,
  ReceivingOrderPaymentStatus,
  ReceivingOrderStatus,
}

final case class ReceivingOrderRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    userId: UUID,
    receivingObjectType: Option[ReceivingOrderObjectType],
    receivingObjectId: Option[UUID],
    status: ReceivingOrderStatus,
    number: String,
    synced: Boolean,
    invoiceNumber: Option[String],
    paymentMethod: Option[ReceivingOrderPaymentMethod],
    paymentStatus: Option[ReceivingOrderPaymentStatus],
    paymentDueDate: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ReceivingOrderUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    userId: Option[UUID],
    receivingObjectType: Option[ReceivingOrderObjectType],
    receivingObjectId: Option[UUID],
    status: Option[ReceivingOrderStatus],
    synced: Option[Boolean],
    invoiceNumber: Option[String],
    paymentMethod: Option[ReceivingOrderPaymentMethod],
    paymentStatus: Option[ReceivingOrderPaymentStatus],
    paymentDueDate: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[ReceivingOrderRecord] {

  def toRecord: ReceivingOrderRecord = {
    require(merchantId.isDefined, s"Impossible to convert ReceivingOrderUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ReceivingOrderUpdate without a location id. [$this]")
    require(userId.isDefined, s"Impossible to convert ReceivingOrderUpdate without a user id. [$this]")
    require(status.isDefined, s"Impossible to convert ReceivingOrderUpdate without a status. [$this]")
    ReceivingOrderRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      userId = userId.get,
      receivingObjectType = receivingObjectType,
      receivingObjectId = receivingObjectId,
      status = status.get,
      number = "",
      synced = synced.getOrElse(false),
      invoiceNumber = invoiceNumber,
      paymentMethod = paymentMethod,
      paymentStatus = paymentStatus,
      paymentDueDate = paymentDueDate,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ReceivingOrderRecord): ReceivingOrderRecord =
    ReceivingOrderRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      receivingObjectType = receivingObjectType.orElse(record.receivingObjectType),
      receivingObjectId = receivingObjectId.orElse(record.receivingObjectId),
      status = status.getOrElse(record.status),
      number = record.number,
      synced = synced.getOrElse(record.synced),
      invoiceNumber = invoiceNumber.orElse(record.invoiceNumber),
      paymentMethod = paymentMethod.orElse(record.paymentMethod),
      paymentStatus = paymentStatus.orElse(record.paymentStatus),
      paymentDueDate = paymentDueDate.orElse(record.paymentDueDate),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
