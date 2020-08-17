package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class DeliveryOrderAccepted(eventName: String, payload: DeliveryOrderAcceptedPayload)
    extends PtDeliveryMsg[Order]

object DeliveryOrderAccepted {

  val eventName = "order_accepted"

  def apply(order: Order)(implicit merchant: MerchantContext): DeliveryOrderAccepted =
    DeliveryOrderAccepted(eventName, DeliveryOrderAcceptedPayload(order))
}

final case class DeliveryOrderAcceptedPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: Order,
  ) extends EntityPayloadLike[Order]

object DeliveryOrderAcceptedPayload {
  def apply(order: Order)(implicit merchant: MerchantContext): DeliveryOrderAcceptedPayload =
    DeliveryOrderAcceptedPayload(
      order.classShortName,
      merchant.id,
      order,
    )
}
