package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.{ ExposedName, StoreType }
import io.paytouch.core.data.model.enums.DeliveryProvider

final case class StoreCreated(eventName: String, payload: StoreCreatedPayload) extends SQSMessage[StoreCreatedData]

object StoreCreated {
  val eventName = "store_created"
}

final case class StoreCreatedPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: StoreCreatedData,
  ) extends EntityPayloadLike[StoreCreatedData]

case class StoreCreatedData(
    locationId: UUID,
    `type`: StoreType,
    provider: Option[DeliveryProvider],
  )
