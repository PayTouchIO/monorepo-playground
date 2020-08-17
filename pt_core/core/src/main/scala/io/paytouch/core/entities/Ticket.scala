package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums._

final case class Ticket(
    id: UUID,
    locationId: UUID,
    orderId: UUID,
    status: TicketStatus,
    show: Boolean,
    routeToKitchenId: UUID,
    orderItems: Seq[OrderItem],
    bundleOrderItems: Seq[OrderItem],
    order: Option[Order],
    startedAt: Option[ZonedDateTime],
    completedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.OrderRoutingTicket
}

final case class TicketCreation(
    locationId: UUID,
    orderId: UUID,
    orderItemIds: Seq[UUID],
    routeToKitchenId: Option[UUID] = None,
  ) extends CreationEntity[Ticket, TicketUpdate] {

  def asUpdate: TicketUpdate =
    TicketUpdate(
      locationId = Some(locationId),
      orderId = Some(orderId),
      orderItemIds = Some(orderItemIds),
      status = Some(TicketStatus.New),
      show = None,
      routeToKitchenId = routeToKitchenId,
    )
}

final case class TicketUpdate(
    locationId: Option[UUID],
    orderId: Option[UUID],
    orderItemIds: Option[Seq[UUID]],
    status: Option[TicketStatus],
    show: Option[Boolean],
    routeToKitchenId: Option[UUID] = None,
  ) extends UpdateEntity[Ticket]

object TicketUpdate {
  def empty =
    TicketUpdate(
      locationId = None,
      orderId = None,
      orderItemIds = None,
      status = None,
      show = None,
      routeToKitchenId = None,
    )
}
