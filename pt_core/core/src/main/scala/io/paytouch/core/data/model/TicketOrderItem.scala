package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class TicketOrderItemRecord(
    id: UUID,
    merchantId: UUID,
    ticketId: UUID,
    orderItemId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class TicketOrderItemUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    ticketId: Option[UUID],
    orderItemId: Option[UUID],
  ) extends SlickMerchantUpdate[TicketOrderItemRecord] {

  def toRecord: TicketOrderItemRecord = {
    require(merchantId.isDefined, s"Impossible to convert TicketOrderItemUpdate without a merchant id. [$this]")
    require(ticketId.isDefined, s"Impossible to convert TicketOrderItemUpdate without a ticket id. [$this]")
    require(orderItemId.isDefined, s"Impossible to convert TicketOrderItemUpdate without a order item id. [$this]")
    TicketOrderItemRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      ticketId = ticketId.get,
      orderItemId = orderItemId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TicketOrderItemRecord): TicketOrderItemRecord =
    TicketOrderItemRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.getOrElse(record.merchantId),
      ticketId = ticketId.getOrElse(record.ticketId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
