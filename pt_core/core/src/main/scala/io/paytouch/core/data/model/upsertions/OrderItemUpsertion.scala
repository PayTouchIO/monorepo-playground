package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class OrderItemUpsertion(
    orderItem: OrderItemUpdate,
    discounts: Option[Seq[OrderItemDiscountUpdate]],
    modifierOptions: Option[Seq[OrderItemModifierOptionUpdate]],
    taxRates: Option[Seq[OrderItemTaxRateUpdate]],
    variantOptions: Option[Seq[OrderItemVariantOptionUpdate]],
    giftCardPassRecipientEmail: Option[String],
  )

object OrderItemUpsertion {
  def empty(orderItem: OrderItemUpdate): OrderItemUpsertion =
    OrderItemUpsertion(
      orderItem,
      discounts = None,
      modifierOptions = None,
      taxRates = None,
      variantOptions = None,
      giftCardPassRecipientEmail = None,
    )
}
