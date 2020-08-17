package io.paytouch.core.entities

final case class ReceiptContext(
    recipientEmail: String,
    order: Order,
    merchant: Merchant,
    locationReceipt: LocationReceipt,
    loyaltyMembership: Option[LoyaltyMembership],
    loyaltyProgram: Option[LoyaltyProgram],
  ) {
  val store = Store(locationReceipt.locationId, locationReceipt.emailImageUrls)
}
