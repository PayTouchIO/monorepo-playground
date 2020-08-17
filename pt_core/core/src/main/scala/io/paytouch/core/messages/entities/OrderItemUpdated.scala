package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.OrderRoutingStatus
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ ExposedEntity, OrderItem, UserContext }

final case class OrderItemUpdated(eventName: String, payload: EntityPayload[OrderItemInfo])
    extends PtNotifierMsg[OrderItemInfo]

object OrderItemUpdated {

  val eventName = "order_item_updated"

  def apply(orderItem: OrderItem, locationId: UUID)(implicit user: UserContext): OrderItemUpdated = {
    val orderItemInfo = OrderItemInfo(orderItem)
    OrderItemUpdated(eventName, EntityPayload(orderItemInfo, Some(locationId)))
  }
}

final case class OrderItemInfo(id: UUID, orderRoutingStatus: Option[OrderRoutingStatus]) extends ExposedEntity {
  val classShortName = ExposedName.OrderItem
}

object OrderItemInfo {

  def apply(orderItem: OrderItem): OrderItemInfo =
    new OrderItemInfo(orderItem.id, orderItem.orderRoutingStatus)
}
