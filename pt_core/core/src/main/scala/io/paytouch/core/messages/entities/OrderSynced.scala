package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.{ Order, UserContext }

final case class OrderSynced(eventName: String, payload: EntityPayload[UUID]) extends PtCoreMsg[UUID]

object OrderSynced {

  val eventName = "order_synced"

  def apply(order: Order)(implicit user: UserContext): OrderSynced = {
    val payload =
      EntityPayload(order.classShortName, order.id, user.merchantId, order.location.map(_.id), user.pusherSocketId)
    OrderSynced(eventName, payload)
  }
}
