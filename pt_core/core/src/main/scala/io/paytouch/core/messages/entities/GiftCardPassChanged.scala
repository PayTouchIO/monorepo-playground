package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ GiftCardPass, UserContext }

final case class GiftCardPassChanged(eventName: String, payload: GiftCardPassPayload) extends PtCoreMsg[GiftCardPass]

object GiftCardPassChanged {

  val eventName = "gift_card_pass_changed"

  def apply(giftCardPass: GiftCardPass)(implicit user: UserContext): GiftCardPassChanged =
    GiftCardPassChanged(eventName, GiftCardPassPayload(user, giftCardPass))
}

final case class GiftCardPassPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: GiftCardPass,
    userContext: UserContext,
  ) extends EntityPayloadLike[GiftCardPass]

object GiftCardPassPayload {
  def apply(userContext: UserContext, giftCardPass: GiftCardPass): GiftCardPassPayload =
    GiftCardPassPayload(giftCardPass.classShortName, userContext.merchantId, giftCardPass, userContext)
}
