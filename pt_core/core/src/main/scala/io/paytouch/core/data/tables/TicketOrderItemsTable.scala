package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.TicketOrderItemRecord

class TicketOrderItemsTable(tag: Tag)
    extends SlickMerchantTable[TicketOrderItemRecord](tag, "order_routing_ticket_order_items") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def ticketId = column[UUID]("order_routing_ticket_id")
  def orderItemId = column[UUID]("order_item_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      ticketId,
      orderItemId,
      createdAt,
      updatedAt,
    ).<>(TicketOrderItemRecord.tupled, TicketOrderItemRecord.unapply)
}
