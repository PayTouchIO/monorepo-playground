package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities.OrderTaxRateUpsertion
import io.paytouch.ordering.entities.{ Cart, CartTaxRate => CartTaxRateEntity }

trait OrderTaxRateConversions {
  protected def toOrderTaxRateUpsertions(cart: Cart): Seq[OrderTaxRateUpsertion] =
    cart.taxRates.map { cartTaxRate =>
      OrderTaxRateUpsertion(
        id = cartTaxRate.id,
        taxRateId = cartTaxRate.taxRateId,
        name = cartTaxRate.name,
        value = cartTaxRate.`value`,
        totalAmount = cartTaxRate.total.amount,
      )
    }
}
