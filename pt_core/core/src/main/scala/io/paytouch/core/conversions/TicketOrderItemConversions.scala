package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.TicketOrderItemUpdate
import io.paytouch.core.entities.UserContext

trait TicketOrderItemConversions {

  def toTicketOrderItemUpdates(
      ticketId: UUID,
      orderItemIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[TicketOrderItemUpdate] =
    orderItemIds.map(toTicketOrderItemUpdate(ticketId, _))

  def toTicketOrderItemUpdate(ticketId: UUID, orderItemId: UUID)(implicit user: UserContext): TicketOrderItemUpdate =
    TicketOrderItemUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      ticketId = Some(ticketId),
      orderItemId = Some(orderItemId),
    )
}
