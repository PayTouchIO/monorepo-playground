package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.TicketRecord
import io.paytouch.core.entities.enums.TicketStatus

class TicketsTable(tag: Tag) extends SlickMerchantTable[TicketRecord](tag, "order_routing_tickets") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def locationId = column[UUID]("location_id")
  def orderId = column[UUID]("order_id")
  def status = column[TicketStatus]("status")
  def show = column[Boolean]("show")
  def routeToKitchenId = column[UUID]("route_to_kitchen_id")

  def startedAt = column[Option[ZonedDateTime]]("started_at")
  def completedAt = column[Option[ZonedDateTime]]("completed_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      orderId,
      status,
      show,
      routeToKitchenId,
      startedAt,
      completedAt,
      createdAt,
      updatedAt,
    ).<>(TicketRecord.tupled, TicketRecord.unapply)
}
