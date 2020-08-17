package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName

final case class OrderReceiptRequestedV2(eventName: String, payload: OrderReceiptRequestedPayloadV2)
    extends PtNotifierMsg[OrderReceiptV2]

final case class OrderReceiptRequestedPayloadV2(
    `object`: ExposedName,
    data: OrderReceiptV2,
    recipientEmail: String,
    merchantId: UUID,
  ) extends EmailEntityPayloadLike[OrderReceiptV2]

final case class OrderReceiptV2(
    order: Order,
    merchant: Merchant,
    locationReceipt: LocationReceipt,
    loyaltyStatus: Option[LoyaltyMembership],
    loyaltyProgram: Option[LoyaltyProgram],
    loyaltyMembership: Option[LoyaltyMembership],
  )

object OrderReceiptRequestedV2 {

  val eventName = "order_receipt_requested_v2"

  def apply(
      order: Order,
      paymentTransactionId: Option[UUID],
      recipientEmail: String,
      merchant: Merchant,
      locationReceipt: LocationReceipt,
      loyaltyMembership: Option[LoyaltyMembership],
      loyaltyProgram: Option[LoyaltyProgram],
    ): OrderReceiptRequestedV2 = {

    // TODO - This filtering of order and paymentTransaction should called from PrepareOrderReceipt
    // once OrderReceiptRequestedV2 is the only version in play
    val filteredOrder = PrepareOrderReceipt.filterOrderPaymentTransactions(order, paymentTransactionId)
    val receipt =
      OrderReceiptV2(filteredOrder, merchant, locationReceipt, loyaltyMembership, loyaltyProgram, loyaltyMembership)
    val payload = OrderReceiptRequestedPayloadV2(filteredOrder.classShortName, receipt, recipientEmail, merchant.id)
    OrderReceiptRequestedV2(eventName, payload)
  }
}
