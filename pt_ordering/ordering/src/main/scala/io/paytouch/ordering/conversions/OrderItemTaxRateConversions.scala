package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities.OrderItemTaxRateUpsertion
import io.paytouch.ordering.entities.CartItem

trait OrderItemTaxRateConversions {
  protected def toOrderItemTaxRateUpsertions(item: CartItem): Seq[OrderItemTaxRateUpsertion] =
    item.taxRates.map { cartTaxRate =>
      OrderItemTaxRateUpsertion(
        id = cartTaxRate.id,
        taxRateId = Some(cartTaxRate.taxRateId),
        name = cartTaxRate.name,
        value = cartTaxRate.`value`,
        totalAmount = Some(cartTaxRate.total.amount),
        applyToPrice = cartTaxRate.applyToPrice,
      )
    }
}
