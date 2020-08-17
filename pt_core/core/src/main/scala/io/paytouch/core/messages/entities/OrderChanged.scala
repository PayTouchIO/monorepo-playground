package io.paytouch.core.messages.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderStatus
import io.paytouch.core.entities.{ Order, OrderRoutingStatusesByType, UserContext }

final case class OrderChanged(eventName: String, payload: EntityPayload[OrderChangeInfo])
    extends PtNotifierMsg[OrderChangeInfo]

object OrderChanged {

  val eventNameCreated = "order_created"
  val eventNameUpdated = "order_updated"

  def created(order: Order)(implicit user: UserContext) = OrderChanged(eventNameCreated, order)

  def updated(order: Order)(implicit user: UserContext) = OrderChanged(eventNameUpdated, order)

  def apply(eventName: String, order: Order)(implicit user: UserContext): OrderChanged = {
    val orderInfoUpdated = OrderChangeInfo(order)
    OrderChanged(
      eventName,
      EntityPayload(
        order.classShortName,
        orderInfoUpdated,
        user.merchantId,
        order.location.map(_.id),
        user.pusherSocketId,
      ),
    )

  }
}

final case class OrderChangeInfo(
    id: UUID,
    orderRoutingStatuses: OrderRoutingStatusesByType,
    status: Option[OrderStatus],
    updatedAt: ZonedDateTime,
  )

object OrderChangeInfo {

  def apply(order: Order): OrderChangeInfo =
    OrderChangeInfo(order.id, order.orderRoutingStatuses, order.status, order.updatedAt)
}
