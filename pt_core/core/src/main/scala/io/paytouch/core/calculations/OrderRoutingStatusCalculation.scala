package io.paytouch.core.calculations

import io.paytouch.core.data.model.TicketRecord
import io.paytouch.core.data.model.enums.OrderRoutingStatus
import io.paytouch.core.entities.enums.TicketStatus

trait OrderRoutingStatusCalculation {

  def inferOrderRoutingStatus(tickets: Seq[TicketRecord]): Option[OrderRoutingStatus] = {
    val ticketStatuses = tickets.map(_.status)
    ticketStatuses match {
      case statuses if statuses.isEmpty                             => None
      case statuses if statuses.forall(_ == TicketStatus.Completed) => Some(OrderRoutingStatus.Completed)
      case statuses if statuses.contains(TicketStatus.InProgress)   => Some(OrderRoutingStatus.Started)
      case statuses if statuses.contains(TicketStatus.New)          => Some(OrderRoutingStatus.New)
      case statuses if statuses.contains(TicketStatus.Canceled)     => Some(OrderRoutingStatus.Canceled)
    }
  }
}
