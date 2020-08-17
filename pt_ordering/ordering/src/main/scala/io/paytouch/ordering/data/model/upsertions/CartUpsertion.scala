package io.paytouch.ordering.data.model.upsertions

import io.paytouch.ordering.data.model._

final case class CartUpsertion(
    cart: CartUpdate,
    cartTaxRates: Seq[CartTaxRateUpdate],
    cartItems: Seq[CartItemUpsertion],
  ) extends UpsertionModel[CartRecord]
