package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class DeliveryOrderRejected(eventName: String, payload: DeliveryOrderRejectedPayload)
    extends PtDeliveryMsg[Order]

object DeliveryOrderRejected {

  val eventName = "order_rejected"

  def apply(order: Order)(implicit merchant: MerchantContext): DeliveryOrderRejected =
    DeliveryOrderRejected(eventName, DeliveryOrderRejectedPayload(order))
}

final case class DeliveryOrderRejectedPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: Order,
  ) extends EntityPayloadLike[Order]

object DeliveryOrderRejectedPayload {
  def apply(order: Order)(implicit merchant: MerchantContext): DeliveryOrderRejectedPayload =
    DeliveryOrderRejectedPayload(
      order.classShortName,
      merchant.id,
      order,
    )
}
