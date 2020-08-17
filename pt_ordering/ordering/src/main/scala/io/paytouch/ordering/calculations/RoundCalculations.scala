package io.paytouch.ordering.calculations

import io.paytouch.ordering.calculations.RoundingUtils._
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.data.model.upsertions._

trait RoundCalculations {
  protected def roundCalculations(upsertion: CartUpsertion): CartUpsertion =
    upsertion.copy(
      cart = roundCalculations(upsertion.cart),
      cartTaxRates = upsertion.cartTaxRates.map(roundCalculations),
      cartItems = upsertion.cartItems.map(roundCalculations),
    )

  private def roundCalculations(cart: CartUpdate): CartUpdate =
    cart.copy(
      subtotalAmount = cart.subtotalAmount.asRounded,
      taxAmount = cart.taxAmount.asRounded,
      totalAmount = cart.totalAmount.asRounded,
    )

  private def roundCalculations(cartTaxRate: CartTaxRateUpdate): CartTaxRateUpdate =
    cartTaxRate.copy(totalAmount = cartTaxRate.totalAmount.asRounded)

  private def roundCalculations(upsertion: CartItemUpsertion): CartItemUpsertion =
    upsertion.copy(
      cartItem = roundCalculations(upsertion.cartItem),
      cartItemTaxRates = upsertion.cartItemTaxRates.map(_.map(roundCalculations)),
    )

  private def roundCalculations(cartItem: CartItemUpdate): CartItemUpdate =
    cartItem.copy(
      taxAmount = cartItem.taxAmount.asRounded,
      calculatedPriceAmount = cartItem.calculatedPriceAmount.asRounded,
      totalPriceAmount = cartItem.totalPriceAmount.asRounded,
    )

  private def roundCalculations(cartItemTaxRate: CartItemTaxRateUpdate): CartItemTaxRateUpdate =
    cartItemTaxRate.copy(totalAmount = cartItemTaxRate.totalAmount.asRounded)
}
