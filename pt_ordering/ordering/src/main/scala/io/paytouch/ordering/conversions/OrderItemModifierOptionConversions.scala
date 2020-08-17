package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities.OrderItemModifierOptionUpsertion
import io.paytouch.ordering.entities.{
  CartItem,
  CartItemModifierOption => CartItemModifierOptionEntity,
  CartItemModifierOptionUpsertion => CartItemModifierOptionUpsertionEntity,
}

trait OrderItemModifierOptionConversions {

  protected def toOrderItemModifierOptionUpsertions(item: CartItem): Seq[OrderItemModifierOptionUpsertion] =
    item.modifierOptions.map(toOrderItemModifierOptionUpsertion)

  protected def toOrderItemModifierOptionUpsertions(
      cartItemModifierOption: Seq[CartItemModifierOptionEntity],
    ): Seq[OrderItemModifierOptionUpsertion] =
    cartItemModifierOption.map(toOrderItemModifierOptionUpsertion)

  protected def toOrderItemModifierOptionUpsertion(
      cartItemModifierOption: CartItemModifierOptionEntity,
    ): OrderItemModifierOptionUpsertion =
    OrderItemModifierOptionUpsertion(
      modifierOptionId = Some(cartItemModifierOption.modifierOptionId),
      name = cartItemModifierOption.name,
      `type` = cartItemModifierOption.`type`,
      price = cartItemModifierOption.price.amount,
      quantity = cartItemModifierOption.quantity,
    )
}
