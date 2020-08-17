package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class OnlineOrderCreated(eventName: String, payload: OnlineOrderCreatedPayload) extends PtNotifierMsg[Order]

object OnlineOrderCreated {

  val eventName = "online_order_created"

  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OnlineOrderCreated =
    OnlineOrderCreated(eventName, OnlineOrderCreatedPayload(receiptContext))
}

final case class OnlineOrderCreatedPayload(
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

object OnlineOrderCreatedPayload {
  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OnlineOrderCreatedPayload =
    OnlineOrderCreatedPayload(
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
