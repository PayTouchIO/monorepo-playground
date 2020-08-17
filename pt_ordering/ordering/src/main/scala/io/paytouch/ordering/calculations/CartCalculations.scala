package io.paytouch.ordering.calculations

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.calculations.RoundingUtils._
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.data.model.upsertions._
import io.paytouch.ordering.entities.enums.OrderType

trait CartCalculations extends CartItemCalculations with CartTaxRateCalculations with RoundCalculations {
  protected def calculationsUpdate(cart: entities.Cart)(implicit store: entities.StoreContext): CartUpsertion = {
    val cartItemUpsertions: Seq[CartItemUpsertion] =
      cart.items.map(calculationsItemUpdate)

    val allItemTaxRateUpdates: Seq[CartItemTaxRateUpdate] =
      cartItemUpsertions.flatMap(_.cartItemTaxRates.toSeq.flatten)

    val cartTaxRateUpdates: Seq[CartTaxRateUpdate] =
      calculationsTaxRateUpdates(cart, allItemTaxRateUpdates)

    val cartItemUpdates =
      cartItemUpsertions.map(_.cartItem)

    val (giftCards, nonGiftCards) =
      cartItemUpdates.partition { update =>
        cart
          .items
          .find(_.id === update.id.getOrDefault)
          .exists(_.isGiftCard)
      }

    val giftCardPriceIncludingTax: BigDecimal =
      giftCards.flatMap(_.totalPriceAmount).sum

    val nonGiftCardPriceIncludingTax: BigDecimal =
      nonGiftCards.flatMap(_.totalPriceAmount).sum

    val totalItemPriceIncludingTax: BigDecimal =
      cartItemUpdates.flatMap(_.totalPriceAmount).sum

    val tip: BigDecimal =
      cart.tip.amount

    val tax: BigDecimal =
      cartItemUpdates.flatMap(_.taxAmount).sum

    val deliveryFeeAmount: Option[BigDecimal] =
      if (cart.orderType == OrderType.Delivery && cart.isNotGiftCardOnly)
        store.deliveryFeeAmount
      else
        None

    val nonGiftCardPriceIncludingTaxWithDelivery =
      nonGiftCardPriceIncludingTax + deliveryFeeAmount.getOrElse(0)

    val resultAfterGiftCardUsageOnNonGiftCardCartItems =
      UseGiftCardPasses(
        appliedGiftCardPasses = cart.appliedGiftCardPasses,
        totalAmountSoFarForNonGiftCardCartItems = nonGiftCardPriceIncludingTaxWithDelivery,
      )

    def withGiftCardsAndTip(soFar: BigDecimal): BigDecimal =
      soFar + giftCardPriceIncludingTax + tip

    val totalAmountWithoutGiftCards =
      withGiftCardsAndTip(nonGiftCardPriceIncludingTaxWithDelivery)

    val totalAmountWithGiftCards =
      withGiftCardsAndTip(resultAfterGiftCardUsageOnNonGiftCardCartItems.total)

    CartUpsertion(
      cart = CartUpdate
        .empty
        .copy(
          id = cart.id.some,
          subtotalAmount = (totalItemPriceIncludingTax - tax).some.nonNegative,
          taxAmount = tax.some.nonNegative,
          totalAmountWithoutGiftCards = totalAmountWithoutGiftCards.some.nonNegative,
          totalAmount = totalAmountWithGiftCards.some.nonNegative,
          deliveryFeeAmount = entities.ResettableBigDecimal(deliveryFeeAmount.some),
          appliedGiftCardPasses = resultAfterGiftCardUsageOnNonGiftCardCartItems.appliedGiftCardPasses.some,
        ),
      cartTaxRates = cartTaxRateUpdates,
      cartItems = cartItemUpsertions,
    ).pipe(roundCalculations)
  }
}
