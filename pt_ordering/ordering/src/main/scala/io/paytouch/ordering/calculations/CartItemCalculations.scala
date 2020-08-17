package io.paytouch.ordering.calculations

import io.paytouch.ordering.data.model._
import io.paytouch.ordering.data.model.upsertions.CartItemUpsertion
import io.paytouch.ordering.entities.enums.ModifierSetType
import io.paytouch.ordering.entities.{ CartItem, CartItemBundleSet, CartItemModifierOption, CartItemTaxRate }
import RoundingUtils._

trait CartItemCalculations {

  case class AnnotatedCartItem(private val cartItem: CartItem) {
    private val bundleSets = cartItem.bundleSets.getOrElse(Seq.empty)
    private val bundleOptionModifierSets = bundleSets.flatMap(_.cartItemBundleOptions.flatMap(_.item.modifierOptions))

    private val aggregatedModifierSets: Seq[CartItemModifierOption] =
      cartItem.modifierOptions ++ bundleOptionModifierSets

    private val modifiersPrice: BigDecimal =
      aggregatedModifierSets.map { modifier =>
        val sign: BigDecimal => BigDecimal = if (modifier.`type` == ModifierSetType.Hold) _.unary_- else identity
        sign(modifier.price.amount * modifier.quantity)
      }.sum

    private val bundleOptionPriceAdjustments = bundleSets.flatMap(_.cartItemBundleOptions.map(_.priceAdjustment)).sum

    val id = cartItem.id
    val quantity = cartItem.quantity
    val isCombo = cartItem.isCombo
    val taxRates = cartItem.taxRates
    val calculatedPriceAmount = cartItem.price.amount
    val baseTotalPriceAmount = (calculatedPriceAmount + modifiersPrice + bundleOptionPriceAdjustments).max(0)
  }

  case class AnnotatedTaxRates(private val cartItem: AnnotatedCartItem) {
    private val notIncludedTaxRates = cartItem.taxRates.filter(_.applyToPrice)
    private val includedTaxRates = cartItem.taxRates diff notIncludedTaxRates
    private val originalBasePrice = {
      val includedPercentage = includedTaxRates.map(_.`value`).sum
      removePercentageFromPrice(cartItem.baseTotalPriceAmount, includedPercentage)
    }

    val includedTaxRateCalculations = calculationsTaxRateUpdates(cartItem, includedTaxRates, originalBasePrice)
    val notIncludedTaxRateCalculations =
      calculationsTaxRateUpdates(cartItem, notIncludedTaxRates, cartItem.baseTotalPriceAmount)
    val taxRateCalculations = notIncludedTaxRateCalculations ++ includedTaxRateCalculations
  }

  protected def calculationsItemUpdate(cartItem: CartItem): CartItemUpsertion = {
    val annotatedCartItem = AnnotatedCartItem(cartItem)
    val annotatedTaxRates = AnnotatedTaxRates(annotatedCartItem)
    CartItemUpsertion(
      cartItem = calculationsCartItemUpdate(annotatedCartItem, annotatedTaxRates),
      cartItemModifierOptions = None,
      cartItemTaxRates = Some(annotatedTaxRates.taxRateCalculations),
      cartItemVariantOptions = None,
    )
  }

  private def calculationsCartItemUpdate(cartItem: AnnotatedCartItem, taxRates: AnnotatedTaxRates): CartItemUpdate = {
    val costAmount: Option[BigDecimal] = if (cartItem.isCombo) Some(0) else None

    val taxAmount = taxRates.taxRateCalculations.flatMap(_.totalAmount).sum

    val nonIncludedTaxAmount = taxRates.notIncludedTaxRateCalculations.flatMap(_.totalAmount).sum

    val totalPriceAmount = cartItem.quantity * cartItem.baseTotalPriceAmount + nonIncludedTaxAmount

    CartItemUpdate
      .empty
      .copy(
        id = Some(cartItem.id),
        costAmount = costAmount,
        taxAmount = Some(taxAmount).nonNegative,
        calculatedPriceAmount = Some(cartItem.calculatedPriceAmount).nonNegative,
        totalPriceAmount = Some(totalPriceAmount).nonNegative,
      )
  }

  private def calculationsTaxRateUpdates(
      cartItem: AnnotatedCartItem,
      taxRates: Seq[CartItemTaxRate],
      basePrice: BigDecimal,
    ): Seq[CartItemTaxRateUpdate] =
    taxRates.map(calculationsTaxRateUpdate(cartItem, _, basePrice))

  private def calculationsTaxRateUpdate(
      cartItem: AnnotatedCartItem,
      taxRate: CartItemTaxRate,
      basePrice: BigDecimal,
    ): CartItemTaxRateUpdate = {
    import RoundingUtils._
    val total = applyPercentageToPrice(price = basePrice, percentage = taxRate.`value`) * cartItem.quantity

    CartItemTaxRateUpdate
      .empty
      .copy(
        id = Some(taxRate.id),
        cartItemId = Some(cartItem.id),
        taxRateId = Some(taxRate.taxRateId),
        applyToPrice = Some(taxRate.applyToPrice),
        totalAmount = Some(total).asRounded.nonNegative, // not ideal, but consistent with how register works
      )
  }

  private def applyPercentageToPrice(price: BigDecimal, percentage: BigDecimal): BigDecimal =
    price * percentage / 100

  private def removePercentageFromPrice(price: BigDecimal, percentage: BigDecimal): BigDecimal =
    price * 100 / (percentage + 100)

}
