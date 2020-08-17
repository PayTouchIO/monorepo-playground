package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object TicketOrderItemSeeds extends Seeds {

  lazy val ticketOrderItemDao = daos.ticketOrderItemDao

  def load(
      tickets: Seq[TicketRecord],
      orderItems: Seq[OrderItemRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[TicketOrderItemRecord]] = {

    val ticketOrderItems = tickets.flatMap { ticket =>
      val orderItemsPerOrder = orderItems.filter(_.orderId == ticket.orderId)
      orderItemsPerOrder.randomAtLeast(1).map { orderItem =>
        TicketOrderItemUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          ticketId = Some(ticket.id),
          orderItemId = Some(orderItem.id),
        )
      }
    }

    ticketOrderItemDao.bulkUpsertByRelIds(ticketOrderItems).extractRecords
  }
}
