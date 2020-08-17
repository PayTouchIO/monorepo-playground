package io.paytouch.ordering.calculations

import java.util.UUID

import cats.implicits._

import io.paytouch.ordering.calculations.RoundingUtils._
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.entities._

trait CartTaxRateCalculations {
  protected def calculationsTaxRateUpdates(
      cart: Cart,
      allItemTaxRateUpdates: Seq[CartItemTaxRateUpdate],
    ): Seq[CartTaxRateUpdate] = {
    val oneForEachRepresentedTaxRateInItems =
      cart
        .items
        .flatMap(_.taxRates)
        .groupBy(_.taxRateId)
        .flatMap {
          case (_, taxRates) => taxRates.headOption
        }
        .toSeq

    oneForEachRepresentedTaxRateInItems
      .map { taxRateTemplate =>
        val matchingUpdates =
          allItemTaxRateUpdates
            .filter(_.taxRateId.contains(taxRateTemplate.taxRateId))

        calculationsTaxRateUpdate(cart, taxRateTemplate, matchingUpdates)
      }
  }

  private def calculationsTaxRateUpdate(
      cart: Cart,
      taxRateTemplate: CartItemTaxRate,
      itemTaxRates: Seq[CartItemTaxRateUpdate],
    ): CartTaxRateUpdate =
    CartTaxRateUpdate
      .empty
      .copy(
        id = None,
        storeId = cart.storeId.some,
        cartId = cart.id.some,
        taxRateId = taxRateTemplate.taxRateId.some,
        name = taxRateTemplate.name.some,
        value = taxRateTemplate.value.some,
        totalAmount = itemTaxRates.flatMap(_.totalAmount).sum.some.nonNegative,
      )
}
