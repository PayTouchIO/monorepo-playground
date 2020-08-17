package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class TicketUpsertion(ticket: TicketUpdate, ticketOrderItems: Option[Seq[TicketOrderItemUpdate]])
    extends UpsertionModel[TicketRecord]
