package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.GiftCard
import io.paytouch.core.entities.enums.ExposedName

final case class GiftCardChanged(eventName: String, payload: GiftCardPayload) extends PtCoreMsg[GiftCard]

object GiftCardChanged {

  val eventName = "gift_card_changed"

  def apply(merchantId: UUID, giftCard: GiftCard): GiftCardChanged =
    GiftCardChanged(eventName, GiftCardPayload(merchantId, giftCard))
}

final case class GiftCardPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: GiftCard,
  ) extends EntityPayloadLike[GiftCard]

object GiftCardPayload {
  def apply(merchantId: UUID, giftCard: GiftCard): GiftCardPayload =
    GiftCardPayload(giftCard.classShortName, merchantId, giftCard)
}
