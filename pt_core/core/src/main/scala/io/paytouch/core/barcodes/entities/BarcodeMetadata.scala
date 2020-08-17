package io.paytouch.core.barcodes.entities

import io.paytouch.core.ServiceConfigurations
import io.paytouch.core.barcodes.entities.enum.BarcodeFormat
import io.paytouch.core.entities.{ GiftCardPass, LoyaltyMembership }

final case class BarcodeMetadata(
    format: BarcodeFormat,
    value: String,
    width: Int = 64,
    height: Int = 64,
    margin: Int = 0,
  )

object BarcodeMetadata {
  def forLoyaltyMembershipSignedUp(loyaltyMembership: LoyaltyMembership) =
    BarcodeMetadata(
      format = BarcodeFormat.Code128,
      value = loyaltyMembership.lookupId,
      width = ServiceConfigurations.barcodeEmailWidth,
      height = ServiceConfigurations.barcodeEmailHeight,
    )

  def forGiftCardPassReceipt(giftCardPass: GiftCardPass): BarcodeMetadata =
    BarcodeMetadata(
      format = BarcodeFormat.Code128,
      value = giftCardPass.lookupId,
      width = ServiceConfigurations.barcodeEmailWidth,
      height = ServiceConfigurations.barcodeEmailHeight,
    )
}
