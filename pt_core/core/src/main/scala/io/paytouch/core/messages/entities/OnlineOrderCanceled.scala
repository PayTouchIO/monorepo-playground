package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName

final case class OnlineOrderCanceled(eventName: String, payload: OnlineOrderCanceledPayload)
    extends PtNotifierMsg[Order]

object OnlineOrderCanceled {
  val eventName = "online_order_canceled"

  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OnlineOrderCanceled =
    OnlineOrderCanceled(eventName, OnlineOrderCanceledPayload(receiptContext))
}

final case class OnlineOrderCanceledPayload(
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

object OnlineOrderCanceledPayload {
  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OnlineOrderCanceledPayload =
    OnlineOrderCanceledPayload(
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
