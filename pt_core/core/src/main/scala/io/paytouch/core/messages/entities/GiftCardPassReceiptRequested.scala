package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName

final case class GiftCardPassReceiptRequested(eventName: String, payload: GiftCardPassReceiptRequestedPayload)
    extends PtNotifierMsg[GiftCardPassReceipt]

final case class GiftCardPassReceiptRequestedPayload(
    `object`: ExposedName,
    data: GiftCardPassReceipt,
    recipientEmail: String,
    merchantId: UUID,
  ) extends EmailEntityPayloadLike[GiftCardPassReceipt]

final case class GiftCardPassReceipt(
    giftCardPass: GiftCardPass,
    merchant: Merchant,
    locationReceipt: LocationReceipt,
    barcodeUrl: String,
    location: Option[Location],
    locationSettings: LocationSettings,
  )

object GiftCardPassReceiptRequested {

  val eventName = "gift_card_pass_receipt_requested"

  def apply(
      giftCardPass: GiftCardPass,
      recipientEmail: String,
      merchant: Merchant,
      locationReceipt: LocationReceipt,
      barcodeUrl: String,
      location: Option[Location],
      locationSettings: LocationSettings,
    ): GiftCardPassReceiptRequested = {
    val receipt = GiftCardPassReceipt(giftCardPass, merchant, locationReceipt, barcodeUrl, location, locationSettings)
    val payload =
      GiftCardPassReceiptRequestedPayload(giftCardPass.classShortName, receipt, recipientEmail, merchant.id)
    GiftCardPassReceiptRequested(eventName, payload)
  }
}
