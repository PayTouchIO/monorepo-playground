package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities.OrderItemVariantOptionUpsertion
import io.paytouch.ordering.entities.{ CartItem, CartItemVariantOption }

trait OrderItemVariantOptionConversions {

  protected def toOrderItemVariantOptionUpsertions(item: CartItem): Seq[OrderItemVariantOptionUpsertion] =
    toOrderItemVariantOptionUpsertions(item.variantOptions)

  protected def toOrderItemVariantOptionUpsertions(
      variantOptions: Seq[CartItemVariantOption],
    ): Seq[OrderItemVariantOptionUpsertion] =
    variantOptions.map { cartItemVariantOption =>
      OrderItemVariantOptionUpsertion(
        variantOptionId = Some(cartItemVariantOption.variantOptionId),
        optionName = Some(cartItemVariantOption.optionName),
        optionTypeName = Some(cartItemVariantOption.optionTypeName),
      )
    }
}
