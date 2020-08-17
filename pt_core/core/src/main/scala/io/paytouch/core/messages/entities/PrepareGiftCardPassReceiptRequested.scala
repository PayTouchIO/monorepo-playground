package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName

final case class PrepareGiftCardPassReceiptRequested(
    eventName: String,
    payload: PrepareGiftCardPassReceiptRequestedPayload,
  ) extends PtCoreMsg[GiftCardPass]

final case class PrepareGiftCardPassReceiptRequestedPayload(
    `object`: ExposedName,
    data: GiftCardPass,
    merchantId: UUID,
    userContext: UserContext,
  ) extends EntityPayloadLike[GiftCardPass]

object PrepareGiftCardPassReceiptRequested {

  val eventName = "prepare_gift_card_pass_receipt_requested"

  def apply(
      giftCardPass: GiftCardPass,
    )(implicit
      userContext: UserContext,
    ): PrepareGiftCardPassReceiptRequested = {
    val payload = PrepareGiftCardPassReceiptRequestedPayload(
      giftCardPass.classShortName,
      giftCardPass,
      userContext.merchantId,
      userContext,
    )
    PrepareGiftCardPassReceiptRequested(eventName, payload)
  }
}
