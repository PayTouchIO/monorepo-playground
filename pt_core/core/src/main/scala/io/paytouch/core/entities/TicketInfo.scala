package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums._

final case class TicketInfo(
    id: UUID,
    locationId: UUID,
    orderId: UUID,
    status: TicketStatus,
    show: Boolean,
    routeToKitchenId: UUID,
    orderItemIds: Seq[UUID],
    startedAt: Option[ZonedDateTime],
    completedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.OrderRoutingTicketInfo
}

object TicketInfo {
  def apply(ticket: Ticket): TicketInfo =
    TicketInfo(
      id = ticket.id,
      locationId = ticket.locationId,
      orderId = ticket.orderId,
      status = ticket.status,
      show = ticket.show,
      routeToKitchenId = ticket.routeToKitchenId,
      orderItemIds = ticket.orderItems.map(_.id),
      startedAt = ticket.startedAt,
      completedAt = ticket.completedAt,
      createdAt = ticket.createdAt,
      updatedAt = ticket.updatedAt,
    )
}
