package io.paytouch.ordering.messages.entities

import java.util.UUID

import io.paytouch.ordering.entities.enums.ExposedName
import io.paytouch.ordering.clients.paytouch.core.entities.enums.StoreType

final case class StoreCreated(eventName: String, payload: StoreCreatedPayload) extends PtCoreMsg[StoreCreatedData]

object StoreCreated {
  val eventName = "store_created"

  def apply(
      merchantId: UUID,
      locationId: UUID,
    ): StoreCreated =
    StoreCreated(eventName, StoreCreatedPayload(merchantId, locationId))
}

final case class StoreCreatedPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: StoreCreatedData,
  ) extends EntityPayloadLike[StoreCreatedData]

object StoreCreatedPayload {
  def apply(
      merchantId: UUID,
      locationId: UUID,
    ): StoreCreatedPayload =
    StoreCreatedPayload(ExposedName.Store, merchantId, StoreCreatedData(locationId, `type` = StoreType.Storefront))
}

case class StoreCreatedData(
    locationId: UUID,
    `type`: StoreType,
  )
