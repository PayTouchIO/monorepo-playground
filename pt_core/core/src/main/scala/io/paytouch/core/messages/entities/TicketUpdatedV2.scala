package io.paytouch.core.messages.entities

import io.paytouch.core.entities._

final case class TicketUpdatedV2(eventName: String, payload: EntityPayload[TicketInfo])
    extends PtNotifierMsg[TicketInfo]

object TicketUpdatedV2 {
  val eventName = "ticket_updated_v2"

  def apply(ticket: Ticket)(implicit user: UserContext): TicketUpdatedV2 =
    TicketUpdatedV2(eventName, EntityPayload(TicketInfo(ticket), Some(ticket.locationId)))
}
