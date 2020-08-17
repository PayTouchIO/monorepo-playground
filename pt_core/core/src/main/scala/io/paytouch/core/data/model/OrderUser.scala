package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OrderUserRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    userId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderUserUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
    userId: Option[UUID],
  ) extends SlickMerchantUpdate[OrderUserRecord] {

  def toRecord: OrderUserRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderUserUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert OrderUserUpdate without a order id. [$this]")
    require(userId.isDefined, s"Impossible to convert OrderUserUpdate without a user id. [$this]")
    OrderUserRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      userId = userId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderUserRecord): OrderUserRecord =
    OrderUserRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderId = orderId.getOrElse(record.orderId),
      userId = userId.getOrElse(record.userId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
