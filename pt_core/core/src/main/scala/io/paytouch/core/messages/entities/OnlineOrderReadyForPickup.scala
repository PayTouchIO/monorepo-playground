package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName

final case class OnlineOrderReadyForPickup(eventName: String, payload: OnlineOrderReadyForPickupPayload)
    extends PtNotifierMsg[Order]

object OnlineOrderReadyForPickup {
  val eventName = "online_order_ready_for_pickup"

  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OnlineOrderReadyForPickup =
    OnlineOrderReadyForPickup(eventName, OnlineOrderReadyForPickupPayload(receiptContext))
}

final case class OnlineOrderReadyForPickupPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: Order,
    recipientEmail: String,
    merchant: Merchant,
    store: Store, // BC: to be deleted once notifier doesn't need/read it anymore
    locationReceipt: LocationReceipt,
    loyaltyMembership: Option[LoyaltyMembership],
    loyaltyProgram: Option[LoyaltyProgram],
  ) extends EntityPayloadLike[Order]

object OnlineOrderReadyForPickupPayload {
  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OnlineOrderReadyForPickupPayload =
    OnlineOrderReadyForPickupPayload(
      receiptContext.order.classShortName,
      merchant.id,
      receiptContext.order,
      receiptContext.recipientEmail,
      receiptContext.merchant,
      receiptContext.store,
      receiptContext.locationReceipt,
      receiptContext.loyaltyMembership,
      receiptContext.loyaltyProgram,
    )
}
