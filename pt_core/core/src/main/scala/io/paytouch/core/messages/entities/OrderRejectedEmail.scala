package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class OrderRejectedEmail(eventName: String, payload: OrderRejectedEmailPayload) extends PtNotifierMsg[Order]

object OrderRejectedEmail {

  val eventName = "order_rejected_email"

  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OrderRejectedEmail =
    OrderRejectedEmail(eventName, OrderRejectedEmailPayload(receiptContext))
}

final case class OrderRejectedEmailPayload(
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

object OrderRejectedEmailPayload {
  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OrderRejectedEmailPayload =
    OrderRejectedEmailPayload(
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
