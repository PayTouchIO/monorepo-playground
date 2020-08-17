package io.paytouch.core.messages.entities

import io.paytouch.core.entities.{ Ticket, TicketInfo, UserContext }

final case class TicketCreatedV2(eventName: String, payload: EntityPayload[TicketInfo])
    extends PtNotifierMsg[TicketInfo]

object TicketCreatedV2 {

  val eventName = "ticket_created_v2"

  def apply(ticket: Ticket)(implicit user: UserContext): TicketCreatedV2 =
    TicketCreatedV2(eventName, EntityPayload(TicketInfo(ticket), Some(ticket.locationId)))
}
