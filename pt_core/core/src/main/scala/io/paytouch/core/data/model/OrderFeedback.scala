package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.utils.UtcTime

final case class OrderFeedbackRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    locationId: Option[UUID],
    customerId: UUID,
    rating: Int,
    body: String,
    read: Boolean,
    receivedAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderFeedbackUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
    locationId: Option[UUID],
    customerId: Option[UUID],
    rating: Option[Int],
    body: Option[String],
    read: Option[Boolean],
    receivedAt: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[OrderFeedbackRecord] {

  def toRecord: OrderFeedbackRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderFeedbackUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert OrderFeedbackUpdate without a order id. [$this]")
    require(customerId.isDefined, s"Impossible to convert OrderFeedbackUpdate without a customer id. [$this]")
    require(rating.isDefined, s"Impossible to convert OrderFeedbackUpdate without a rating. [$this]")
    require(body.isDefined, s"Impossible to convert OrderFeedbackUpdate without a body. [$this]")
    OrderFeedbackRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      locationId = locationId,
      customerId = customerId.get,
      rating = rating.get,
      body = body.get,
      read = read.getOrElse(false),
      receivedAt = receivedAt.getOrElse(UtcTime.now),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderFeedbackRecord): OrderFeedbackRecord =
    OrderFeedbackRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderId = orderId.getOrElse(record.orderId),
      locationId = locationId.orElse(record.locationId),
      customerId = customerId.getOrElse(record.customerId),
      rating = rating.getOrElse(record.rating),
      body = body.getOrElse(record.body),
      read = read.getOrElse(record.read),
      receivedAt = receivedAt.getOrElse(UtcTime.now),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
