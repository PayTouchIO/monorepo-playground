package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.PaymentStatus
import io.paytouch.ordering.data.model.upsertions.{ CartItemUpsertion => CartItemUpsertionModel }
import io.paytouch.ordering.entities.{ Cart, CartItem => CartItemEntity, CartItemUpsertion => CartItemUpsertionEntity }

trait OrderItemConversions
    extends OrderItemModifierOptionConversions
       with OrderItemVariantOptionConversions
       with OrderItemTaxRateConversions {
  protected def toOrderItemUpsertions(cart: Cart): Seq[OrderItemUpsertion] =
    cart.items.map(toOrderItemUpsertion)

  private def toOrderItemUpsertion(cartItem: CartItemEntity): OrderItemUpsertion =
    OrderItemUpsertion(
      id = cartItem.id,
      productId = Some(cartItem.product.id),
      productName = Some(cartItem.product.name),
      productDescription = cartItem.product.description,
      quantity = Some(cartItem.quantity),
      unit = Some(cartItem.unit),
      paymentStatus = Some(PaymentStatus.Pending),
      priceAmount = Some(cartItem.price.amount),
      costAmount = cartItem.cost.map(_.amount),
      taxAmount = Some(cartItem.tax.amount),
      discountAmount = None,
      calculatedPriceAmount = Some(cartItem.calculatedPrice.amount),
      totalPriceAmount = Some(cartItem.totalPrice.amount),
      modifierOptions = toOrderItemModifierOptionUpsertions(cartItem),
      variantOptions = toOrderItemVariantOptionUpsertions(cartItem),
      notes = cartItem.notes,
      taxRates = toOrderItemTaxRateUpsertions(cartItem),
      giftCardPassRecipientEmail = cartItem.giftCardData.map(_.recipientEmail),
    )
}
