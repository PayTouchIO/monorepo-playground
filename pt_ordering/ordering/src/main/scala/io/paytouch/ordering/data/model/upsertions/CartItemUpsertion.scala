package io.paytouch.ordering.data.model.upsertions

import io.paytouch.ordering.data.model._

final case class CartItemUpsertion(
    cartItem: CartItemUpdate,
    cartItemModifierOptions: Option[Seq[CartItemModifierOptionUpdate]],
    cartItemTaxRates: Option[Seq[CartItemTaxRateUpdate]],
    cartItemVariantOptions: Option[Seq[CartItemVariantOptionUpdate]],
  ) extends UpsertionModel[CartItemRecord]
