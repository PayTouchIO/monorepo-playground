package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.entities.enums.ExposedName

final case class StoresActiveChanged(eventName: String, payload: StoresActivePayload)
    extends SQSMessage[Seq[UpdateActiveItem]]

object StoresActiveChanged {

  val eventName = "stores_active_changed"

  def apply(merchantId: UUID, locationItems: Seq[UpdateActiveItem]): StoresActiveChanged =
    StoresActiveChanged(eventName, StoresActivePayload(merchantId, locationItems))
}

final case class StoresActivePayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: Seq[UpdateActiveItem],
  ) extends EntityPayloadLike[Seq[UpdateActiveItem]]

object StoresActivePayload {
  def apply(merchantId: UUID, locationItems: Seq[UpdateActiveItem]): StoresActivePayload =
    StoresActivePayload(ExposedName.Store, merchantId, locationItems)
}
