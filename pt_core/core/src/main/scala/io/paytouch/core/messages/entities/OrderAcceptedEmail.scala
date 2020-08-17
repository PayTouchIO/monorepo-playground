package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities._

final case class OrderAcceptedEmail(eventName: String, payload: OrderAcceptedEmailPayload) extends PtNotifierMsg[Order]

object OrderAcceptedEmail {

  val eventName = "order_accepted_email"

  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OrderAcceptedEmail =
    OrderAcceptedEmail(eventName, OrderAcceptedEmailPayload(receiptContext))
}

final case class OrderAcceptedEmailPayload(
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

object OrderAcceptedEmailPayload {
  def apply(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): OrderAcceptedEmailPayload =
    OrderAcceptedEmailPayload(
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
