package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.entities.enums.ExposedName

final case class RapidoChanged(eventName: String, payload: RapidoPayload) extends SQSMessage[Seq[UpdateActiveItem]]

object RapidoChanged {

  val eventName = "rapido_changed"

  def apply(merchantId: UUID, locationItems: Seq[UpdateActiveItem]): RapidoChanged =
    RapidoChanged(eventName, RapidoPayload(merchantId, locationItems))
}

final case class RapidoPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: Seq[UpdateActiveItem],
  ) extends EntityPayloadLike[Seq[UpdateActiveItem]]

object RapidoPayload {
  def apply(merchantId: UUID, locationItems: Seq[UpdateActiveItem]): RapidoPayload =
    RapidoPayload(ExposedName.Store, merchantId, locationItems)
}
