package io.paytouch.ordering.calculations

import java.util.UUID

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ ArticleType, CartItemType }
import io.paytouch.ordering.clients.paytouch.core.entities.{ ModifierSet, _ }
import io.paytouch.ordering.data.model.upsertions.{
  CartItemUpsertion => CartItemUpsertionModel,
  CartUpsertion => CartUpsertionModel,
}
import io.paytouch.ordering.data.model.{ CartItemTaxRateUpdate, CartTaxRateUpdate }
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ ModifierSetType, OrderType, UnitType }
import io.paytouch.ordering.utils.{ CommonArbitraries, PaytouchSpec }
import org.specs2.specification.Scope

final case class ProductRelevantFieldsOnly(
    price: BigDecimal,
    cost: Option[BigDecimal],
    taxRates: Seq[TaxRate],
    modifierSets: Seq[ModifierSet],
    `type`: CartItemType = CartItemType.Product,
  )

final case class ItemDetail(
    product: ProductRelevantFieldsOnly,
    quantity: Int,
    modifierOptionQuantities: Map[ModifierOption, BigDecimal],
    bundleOptionModifiersAndQuantities: Map[BundleOption, Map[ModifierOption, BigDecimal]] = Map.empty,
  )

abstract class CartCalculationsOpsSpec extends PaytouchSpec with CommonArbitraries {
  abstract class CartCalculationsSpecContext extends CartCalculations with Scope with CartFixtures {
    def assertCartTotals(
        tax: BigDecimal,
        subtotal: BigDecimal,
        total: BigDecimal,
        totalWithoutGiftCards: Option[BigDecimal] = None,
        deliveryFeeAmount: ResettableBigDecimal = ResettableBigDecimal.reset,
      )(implicit
        upsertion: CartUpsertionModel,
      ) = {
      val update = upsertion.cart
      update.id.optT ==== Some(cart.id)
      update.currency ==== None
      update.taxAmount ==== Some(tax)
      update.subtotalAmount ==== Some(subtotal)

      totalWithoutGiftCards.foreach { t =>
        update.totalAmountWithoutGiftCards.get must beCloseTo(t within 10.significantFigures)
      }

      update.totalAmount.get must beCloseTo(total within 10.significantFigures)
      update.tipAmount ==== None
      update.deliveryFeeAmount ==== deliveryFeeAmount
    }

    def assertCartTaxRateTotals(coreEntity: TaxRate, total: BigDecimal)(implicit upsertion: CartUpsertionModel) = {
      val (entity, update) = selectCartTaxRateEntityAndUpdate(coreEntity, cart.taxRates, upsertion.cartTaxRates)
      update.taxRateId ==== Some(entity.taxRateId)
      update.value ==== Some(entity.value)
      update.totalAmount ==== Some(total)
    }

    def assertCartItemsSize(size: Int)(implicit upsertion: CartUpsertionModel) = {
      upsertion.cartItems.size ==== cart.items.size
      upsertion.cartItems.size ==== size
    }

    def assertCartItem(position: Int)(implicit upsertion: CartUpsertionModel): (CartItem, CartItemUpsertionModel) = {
      val cartItemUpsertion = upsertion.cartItems(position)
      val cartItem = cart.items(position)

      cartItemUpsertion.cartItemModifierOptions ==== None
      cartItemUpsertion.cartItemVariantOptions ==== None

      (cartItem, cartItemUpsertion)
    }

    def assertCartItemTotals(
        tax: BigDecimal,
        calculatedPrice: BigDecimal,
        total: BigDecimal,
        cost: Option[BigDecimal] = None,
      )(
        entity: CartItem,
        upsertion: CartItemUpsertionModel,
      ) = {
      val update = upsertion.cartItem
      update.id.optT ==== Some(entity.id)
      update.quantity ==== None
      update.priceAmount ==== None
      update.costAmount ==== cost
      update.taxAmount ==== Some(tax)
      update.calculatedPriceAmount ==== Some(calculatedPrice)
      update.totalPriceAmount ==== Some(total)
    }

    def assertCartItemTaxRateTotals(
        coreEntity: TaxRate,
        total: BigDecimal,
      )(
        cartItem: CartItem,
        upsertion: CartItemUpsertionModel,
      ) = {
      val (entity, update) = selectCartItemTaxRateEntityAndUpdate(
        coreEntity,
        cartItem.taxRates,
        upsertion.cartItemTaxRates.getOrElse(Seq.empty),
      )
      update.id.optT ==== Some(entity.id)
      update.cartItemId ==== Some(cartItem.id)
      update.taxRateId ==== Some(entity.taxRateId)
      update.value ==== None
      update.totalAmount ==== Some(total)
      update.applyToPrice ==== Some(entity.applyToPrice)
    }

    def selectCartTaxRateEntityAndUpdate(
        coreTaxRate: TaxRate,
        entities: Seq[CartTaxRate],
        updates: Seq[CartTaxRateUpdate],
      ): (CartTaxRate, CartTaxRateUpdate) =
      selectEntityAndUpdateByTaxRate(coreTaxRate, entities, updates)(
        idExtractorE = _.taxRateId,
        idExtractorU = _.taxRateId,
      )

    def selectCartItemTaxRateEntityAndUpdate(
        coreTaxRate: TaxRate,
        entities: Seq[CartItemTaxRate],
        updates: Seq[CartItemTaxRateUpdate],
      ): (CartItemTaxRate, CartItemTaxRateUpdate) =
      selectEntityAndUpdateByTaxRate(coreTaxRate, entities, updates)(
        idExtractorE = _.taxRateId,
        idExtractorU = _.taxRateId,
      )

    private def selectEntityAndUpdateByTaxRate[E, U](
        coreTaxRate: TaxRate,
        entities: Seq[E],
        updates: Seq[U],
      )(
        idExtractorE: E => UUID,
        idExtractorU: U => Option[UUID],
      ): (E, U) = {
      val entity = entities.find(idExtractorE(_) == coreTaxRate.id).get
      val update = updates.find(idExtractorU(_).contains(coreTaxRate.id)).get
      entity -> update
    }
  }

  trait CartFixtures extends CoreFixtures {
    type ModifierOptionQuantity = Map[ModifierOption, BigDecimal]
    def tip: MonetaryAmount
    def itemsDetails: Seq[ItemDetail]
    def deliveryFee = 5.USD

    implicit lazy val storeContext = StoreContext(
      id = UUID.randomUUID,
      currency = USD,
      merchantId = UUID.randomUUID,
      locationId = UUID.randomUUID,
      deliveryFeeAmount = Some(deliveryFee.amount),
      deliveryMinAmount = None,
      deliveryMaxAmount = None,
      paymentMethods = Seq.empty,
    )

    private def buildInitialCartTaxRate(coreTaxRate: TaxRate) =
      CartTaxRate(
        id = UUID.randomUUID,
        taxRateId = coreTaxRate.id,
        name = coreTaxRate.name,
        `value` = coreTaxRate.value,
        total = 0 USD,
      )

    val allItemsTaxRates = itemsDetails.flatMap(_.product.taxRates).distinct

    private def buildInitialCartItemTaxRate(coreTaxRate: TaxRate) =
      CartItemTaxRate(
        id = UUID.randomUUID,
        taxRateId = coreTaxRate.id,
        name = coreTaxRate.name,
        `value` = coreTaxRate.value,
        total = 0 USD,
        applyToPrice = coreTaxRate.applyToPrice,
      )

    private def buildInitialCartItemModifierOption(
        quantity: BigDecimal,
        coreModifierSet: ModifierSet,
        coreModifierOption: ModifierOption,
      ) =
      CartItemModifierOption(
        id = UUID.randomUUID,
        modifierOptionId = coreModifierOption.id,
        name = coreModifierOption.name,
        `type` = coreModifierSet.`type`,
        price = coreModifierOption.price,
        quantity = quantity,
      )

    private def buildInitialCartItemModifierOptions(data: Map[ModifierOption, BigDecimal]) =
      data.flatMap {
        case (option, quantity) =>
          modifierOptionToModifierSet.get(option).map { set =>
            buildInitialCartItemModifierOption(quantity, set, option)
          }
      }.toSeq

    private def buildInitialCartItemBundleSets(data: Map[BundleOption, ModifierOptionQuantity]) =
      data
        .keys
        .flatMap { bundleOption =>
          bundleOptionToBundleSet
            .get(bundleOption)
            .map(bundleSet => buildInitialCartItemBundleSet(bundleSet, data))
        }
        .toSeq

    private def buildInitialCartItemBundleSet(
        bundleSet: BundleSet,
        bundleOptionModifiers: Map[BundleOption, ModifierOptionQuantity],
      ): CartItemBundleSet =
      CartItemBundleSet(
        name = bundleSet.name,
        bundleSetId = bundleSet.id,
        position = bundleSet.position,
        cartItemBundleOptions = bundleOptionModifiers.map {
          case (bundleOption, modifierSetAndOptionData) =>
            buildInitialCartItemBundleOption(bundleOption, modifierSetAndOptionData)
        }.toSeq,
      )

    private def buildInitialCartItemBundleOption(
        bundleOption: BundleOption,
        modifierSetAndOptionData: ModifierOptionQuantity,
      ): CartItemBundleOption =
      CartItemBundleOption(
        bundleOptionId = bundleOption.id,
        item = CartItemBundleOptionItem(
          product = CartItemProduct(
            id = UUID.randomUUID,
            name = "Foo",
            description = Some("Bar"),
          ),
          quantity = 1,
          unit = UnitType.Unit,
          cost = None,
          notes = None,
          modifierOptions = buildInitialCartItemModifierOptions(modifierSetAndOptionData),
          variantOptions = Seq.empty,
        ),
        priceAdjustment = bundleOption.priceAdjustment,
        position = bundleOption.position,
      )

    @scala.annotation.nowarn("msg=Auto-application")
    private val items: Seq[CartItem] = itemsDetails.map { itemDetail =>
      random[CartItem].copy(
        id = UUID.randomUUID,
        quantity = itemDetail.quantity,
        price = itemDetail.product.price.USD,
        cost = itemDetail.product.cost.map(_.USD),
        modifierOptions = buildInitialCartItemModifierOptions(itemDetail.modifierOptionQuantities),
        taxRates = itemDetail.product.taxRates.map(buildInitialCartItemTaxRate),
        bundleSets = Some(buildInitialCartItemBundleSets(itemDetail.bundleOptionModifiersAndQuantities)),
        `type` = itemDetail.product.`type`,
        giftCardData =
          if (itemDetail.product.`type` == CartItemType.GiftCard)
            random[GiftCardData].some
          else
            None,
      )
    }

    @scala.annotation.nowarn("msg=Auto-application")
    val cart = random[Cart].copy(
      orderType = OrderType.TakeOut,
      taxRates = allItemsTaxRates.map(buildInitialCartTaxRate),
      items = items,
      tip = tip,
      deliveryFee = Some(deliveryFee),
    )
  }

  trait CoreFixtures {
    // We have a script to create fixtures on dev, so we can cross-check calculations with register (see README).
    // If you update the values below, also update `ordering/src/main/resources/seeds.rb`

    val applyA = TaxRate(UUID.randomUUID, "ApplyA", 25.00, true, Map.empty)
    val applyB = TaxRate(UUID.randomUUID, "ApplyB", 12.00, true, Map.empty)
    val includedA =
      TaxRate(UUID.randomUUID, "IncludedA", 5.00, false, Map.empty)
    val includedB =
      TaxRate(UUID.randomUUID, "IncludedB", 10.00, false, Map.empty)

    val myAddA = ModifierOption(UUID.randomUUID, "MyAddA", 1.50 USD, 1337.some, 0, true)
    val myAddB = ModifierOption(UUID.randomUUID, "MyAddB", 1.25 USD, 1338.some, 1, true)
    val myAdd = ModifierSet(
      id = UUID.randomUUID,
      `type` = ModifierSetType.Addon,
      name = "MyAdd",
      minimumOptionCount = 1,
      maximumOptionCount = 1.some,
      singleChoice = true,
      force = true,
      locationOverrides = Map.empty,
      options = Seq(myAddA, myAddB),
    )

    val myHoldA = ModifierOption(UUID.randomUUID, "MyHoldA", 0.25 USD, 1337.some, 0, true)
    val myHoldB = ModifierOption(UUID.randomUUID, "MyHoldB", 0.35 USD, 1338.some, 1, true)
    val myHold = ModifierSet(
      id = UUID.randomUUID,
      `type` = ModifierSetType.Hold,
      name = "MyHold",
      minimumOptionCount = 1,
      maximumOptionCount = 1.some,
      singleChoice = true,
      force = true,
      locationOverrides = Map.empty,
      options = Seq(myHoldA, myHoldB),
    )

    val myNeutralA =
      ModifierOption(UUID.randomUUID, "MyNeutralA", 0 USD, 1337.some, 0, true)
    val myNeutralB =
      ModifierOption(UUID.randomUUID, "MyNeutralB", 0 USD, 1338.some, 1, true)
    val myNeutral = ModifierSet(
      id = UUID.randomUUID,
      `type` = ModifierSetType.Neutral,
      name = "MyNeutral",
      minimumOptionCount = 1,
      maximumOptionCount = 1.some,
      singleChoice = true,
      force = true,
      locationOverrides = Map.empty,
      options = Seq(myNeutralA, myNeutralB),
    )

    private val allModifierSets = Seq(myAdd, myHold, myNeutral)
    val modifierOptionToModifierSet: Map[ModifierOption, ModifierSet] =
      (for {
        modifierSet <- allModifierSets
        modifierOption <- modifierSet.options
      } yield modifierOption -> modifierSet).toMap

    val myBundleOption1 = BundleOption(
      UUID.randomUUID,
      ArticleInfo(id = UUID.randomUUID, name = "product 1 for bundle", sku = None, upc = None, options = None),
      priceAdjustment = 0,
      position = 1,
    )
    val myBundleOption2 = BundleOption(
      UUID.randomUUID,
      ArticleInfo(id = UUID.randomUUID, name = "product 2 for bundle", sku = None, upc = None, options = None),
      priceAdjustment = 5,
      position = 2,
    )
    val myBundleSet = BundleSet(
      UUID.randomUUID,
      name = Some("MyBundleSet"),
      position = 1,
      minQuantity = 1,
      maxQuantity = 1,
      options = Seq(myBundleOption1, myBundleOption2),
    )

    val allBundleSets = Seq(myBundleSet)
    val bundleOptionToBundleSet: Map[BundleOption, BundleSet] =
      (for {
        bundleSet <- allBundleSets
        bundleOption <- bundleSet.options
      } yield bundleOption -> bundleSet).toMap

    val productScenarioA = ProductRelevantFieldsOnly(
      price = 1,
      cost = Some(0.5),
      taxRates = Seq(applyA, applyB),
      modifierSets = Seq(myAdd, myHold),
    )

    val productScenarioB = ProductRelevantFieldsOnly(
      price = 1,
      cost = Some(0.5),
      taxRates = Seq(applyA, applyB, includedA, includedB),
      modifierSets = Seq(myAdd, myHold),
    )

    val productScenarioC1 = ProductRelevantFieldsOnly(
      price = 1,
      cost = Some(0.5),
      taxRates = Seq(applyA, applyB),
      modifierSets = Seq(myAdd, myHold),
    )

    val productScenarioC2 = ProductRelevantFieldsOnly(
      price = 1,
      cost = Some(0.5),
      taxRates = Seq(applyA, applyB),
      modifierSets = Seq(myAdd, myHold),
    )

    val productScenarioD = ProductRelevantFieldsOnly(
      price = 1,
      cost = Some(0.5),
      taxRates = Seq(applyA, applyB),
      modifierSets = Seq(myNeutral),
    )

    val bundleScenarioE = ProductRelevantFieldsOnly(
      price = 2,
      cost = Some(1),
      taxRates = Seq(applyA),
      modifierSets = Seq.empty,
    )

    val productScenarioF = ProductRelevantFieldsOnly(
      price = 10,
      cost = None,
      taxRates = Seq.empty,
      modifierSets = Seq.empty,
      `type` = CartItemType.GiftCard,
    )
  }
}
