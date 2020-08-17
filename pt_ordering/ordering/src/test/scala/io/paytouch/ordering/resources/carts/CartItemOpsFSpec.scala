package io.paytouch.ordering.resources.carts

import java.util.UUID

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.data.model.CartItemRecord
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ OrderType, UnitType }
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory, _ }

@scala.annotation.nowarn("msg=Auto-application")
abstract class CartItemOpsFSpec extends CartsFSpec with CommonArbitraries {
  abstract class CartItemOpsFSpecContext extends CartResourceFSpecContext {
    protected def assertNewItemAdded(
        entity: Cart,
        creation: CartItemCreation,
        product: Product,
        modifierOptions: Seq[ModifierOption] = Seq.empty,
        variantOptions: Seq[VariantOptionWithType] = Seq.empty,
        taxRates: Seq[TaxRate] = Seq.empty,
        itemsSize: Integer = 1,
        expectedBundleSetIdBundleOptionIdVariantOptionIds: Map[UUID, Map[UUID, Seq[UUID]]] = Map.empty,
      ) = {
      entity.items.size === itemsSize
      val newItem = entity.items.last
      newItem.product.name ==== product.name
      newItem.product.description ==== product.description
      newItem.unit ==== product.unit
      newItem.quantity ==== creation.quantity
      newItem.notes ==== creation.notes
      newItem.giftCardData ==== creation.giftCardData

      assertModifierOptionIds(newItem, modifierOptions.map(_.id))
      assertVariantOptionIds(newItem, variantOptions.map(_.id))
      assertTaxRateIds(newItem, taxRates.map(_.id))
      assertBundleSetup(newItem, expectedBundleSetIdBundleOptionIdVariantOptionIds)
    }

    protected def assertItemUpdated(
        entity: Cart,
        update: CartItemUpdate,
        cartItem: CartItemRecord,
        modifierOptionIds: Seq[UUID],
        variantOptionIds: Seq[UUID] = Seq.empty,
        taxRateIds: Seq[UUID] = Seq.empty,
      ) = {
      val updatedItem: CartItem =
        entity.items.find(_.id === cartItem.id).get

      if (update.quantity.isDefined) update.quantity.get ==== updatedItem.quantity
      if (update.notes.isDefined) updatedItem.notes ==== update.notes
      if (update.giftCardData.isDefined) update.giftCardData === update.giftCardData

      assertModifierOptionIds(updatedItem, modifierOptionIds)
      assertVariantOptionIds(updatedItem, variantOptionIds)
      assertTaxRateIds(updatedItem, taxRateIds)
    }

    private def assertModifierOptionIds(cartItem: CartItem, modifierOptionIds: Seq[UUID]) = {
      val modifierOptionRecords = cartItemModifierOptionDao.findByCartItemIds(Seq(cartItem.id)).await

      modifierOptionRecords.map(_.modifierOptionId) must containTheSameElementsAs(modifierOptionIds)
    }

    private def assertTaxRateIds(cartItem: CartItem, taxRateIds: Seq[UUID]) = {
      val taxRateRecords = cartItemTaxRateDao.findByCartItemIds(Seq(cartItem.id)).await

      taxRateRecords.map(_.taxRateId) must containTheSameElementsAs(taxRateIds)
    }

    private def assertVariantOptionIds(cartItem: CartItem, variantOptionIds: Seq[UUID]) = {
      val variantOptionRecords = cartItemVariantOptionDao.findByCartItemIds(Seq(cartItem.id)).await

      variantOptionRecords.map(_.variantOptionId) must containTheSameElementsAs(variantOptionIds)
    }

    private def assertBundleSetup(
        cartItem: CartItem,
        expectedBundleSetOptionVariantOptionIds: Map[UUID, Map[UUID, Seq[UUID]]],
      ) = {
      val carItemRecord = cartItemDao.findById(cartItem.id).await.get
      val bundleSets = carItemRecord.bundleSets.getOrElse(Seq.empty)
      val expectedBundleSetIds = expectedBundleSetOptionVariantOptionIds.keys.toSeq
      val expectedBundleOptionIds = expectedBundleSetOptionVariantOptionIds.flatMap { case (k, v) => v.keys.toSeq }

      bundleSets.map(_.bundleSetId) must containTheSameElementsAs(expectedBundleSetIds)

      forall(bundleSets) { bundleSet =>
        val bundleOptionVariantOptionIds =
          expectedBundleSetOptionVariantOptionIds.getOrElse(bundleSet.bundleSetId, Map.empty)
        val expectedBundleOptionIds = bundleOptionVariantOptionIds.keys.toSeq

        bundleSet.cartItemBundleOptions.map(_.bundleOptionId) must containTheSameElementsAs(expectedBundleOptionIds)
        forall(bundleSet.cartItemBundleOptions) { bundleOption =>
          val expectedVariantOptionIds = bundleOptionVariantOptionIds.getOrElse(bundleOption.bundleOptionId, Seq.empty)

          bundleOption.item.variantOptions.map(_.variantOptionId) must containTheSameElementsAs(
            expectedVariantOptionIds,
          )
        }
      }
    }

    protected def assertItemMerged(
        entity: Cart,
        creation: CartItemCreation,
        cartItem: CartItemRecord,
      ) = {
      entity.items.size ==== 1
      val newItem = entity.items.head
      newItem.quantity ==== creation.quantity + cartItem.quantity
    }

    protected def assertCartTotals(
        entity: Cart,
        subtotal: Option[BigDecimal] = None,
        tax: Option[BigDecimal] = None,
        total: Option[BigDecimal] = None,
      ) = {
      if (subtotal.isDefined)
        entity.subtotal.amount ==== subtotal.get

      if (tax.isDefined) {
        entity.tax.amount ==== tax.get
        entity.taxRates.map(_.total.amount).sum ==== tax.get
      }

      if (total.isDefined)
        entity.total.amount ==== total.get
    }
  }

  trait ProductFixtures { self: MultipleLocationFixtures =>
    val id = UUID.randomUUID

    lazy val cart = Factory
      .cart(
        romeStore,
        subtotalAmount = 0.somew,
        deliveryFeeAmount = 0.somew,
        taxAmount = 0.somew,
        tipAmount = 0.somew,
        totalAmount = 0.somew,
        orderType = OrderType.TakeOut.some,
      )
      .create

    implicit val storeContext = StoreContext.fromRecord(romeStore)

    lazy val variantOption = random[VariantOptionWithType]

    val taxRate1 = random[TaxRate].copy(
      id = UUID.randomUUID,
      name = "Apply 7.5%",
      value = 7.5,
      applyToPrice = true,
      locationOverrides = Map(
        romeId -> ItemLocation(active = true),
      ),
    )

    val taxRate2 = random[TaxRate].copy(
      id = UUID.randomUUID,
      name = "Included 1.5%",
      value = 1.5,
      applyToPrice = false,
      locationOverrides = Map(
        romeId -> ItemLocation(active = true),
      ),
    )

    val taxRate3 = random[TaxRate].copy(
      id = UUID.randomUUID,
      name = "Disabled",
      value = 20,
      applyToPrice = true,
      locationOverrides = Map(
        romeId -> ItemLocation(active = false),
      ),
    )

    val taxRates = Seq(taxRate1, taxRate2, taxRate3)
    val taxRatesActive = taxRates.filter(_.locationOverrides.values.exists(_.active))

    lazy val variantOptions: Seq[VariantOptionWithType] = Seq.empty

    lazy val modifiers: Seq[ModifierSet] = Seq.empty

    lazy val bundleSets: Seq[BundleSet] = Seq.empty

    lazy val isCombo = false

    lazy val quantity = BigDecimal(1000)
    lazy val sellOutOfStock = false
    lazy val trackInventory = true

    lazy val product: Product = {
      val unitType: UnitType =
        genUnitType.instance

      random[Product]
        .copy(
          id = UUID.randomUUID,
          unit = unitType,
          isCombo = isCombo,
          locationOverrides = Map(
            romeId -> ProductLocation(
              price = 10.$$$,
              cost = 1.$$$.some,
              unit = unitType,
              active = true,
              taxRates = taxRates,
              stock = Stock(
                quantity = quantity,
                sellOutOfStock = sellOutOfStock,
              ).some,
            ),
          ),
          options = Seq(variantOption),
          modifiers = modifiers.some,
          bundleSets = bundleSets,
          trackInventory = trackInventory,
        )
    }

    lazy val giftCardProduct: Product =
      Product(
        id = UUID.randomUUID,
        `type` = ArticleType.GiftCard,
        scope = ArticleScope.Product,
        isCombo = false,
        name = "Gift Card",
        description = None,
        price = 0.$$$,
        variants = None,
        variantProducts = None,
        unit = UnitType.`Unit`,
        isVariantOfProductId = None,
        hasVariant = false,
        active = true,
        locationOverrides = Map.empty,
        options = Seq.empty,
        modifierIds = None,
        modifiers = None,
        modifierPositions = None,
        avatarBgColor = None,
        avatarImageUrls = Seq.empty,
        hasParts = false,
        trackInventory = false,
        categoryOptions = None,
        categoryPositions = Seq.empty,
        bundleSets = Seq.empty,
        priceRange = None,
      )

    lazy val modifierOptionCreations: Seq[CartItemModifierOptionCreation] = Seq.empty

    lazy val bundleSetCreations: Option[Seq[CartItemBundleSetCreation]] = None

    def cartItemCreationQuantity: Int = 2

    lazy val creation: CartItemCreation =
      random[CartItemCreation]
        .copy(
          cartId = cart.id,
          productId = product.id,
          quantity = cartItemCreationQuantity,
          modifierOptions = modifierOptionCreations,
          bundleSets = bundleSetCreations,
          `type` = CartItemType.Product,
          giftCardData = None,
        )

    lazy val giftCardCreation: GiftCardCartItemCreation =
      GiftCardCartItemCreation(
        cartId = creation.cartId,
        productId = giftCardProduct.id,
        giftCardData = random[GiftCardData],
      )

    def randomBundledProduct(quantity: BigDecimal = 1000): Product = {
      val unitType = genUnitType.instance
      random[Product].copy(
        id = UUID.randomUUID,
        unit = unitType,
        isCombo = false,
        locationOverrides = Map(
          romeId -> ProductLocation(
            price = 10 $$$,
            cost = Some(1 $$$),
            unit = unitType,
            active = true,
            taxRates = taxRates,
            stock = Some(
              Stock(
                quantity = quantity,
                sellOutOfStock = false,
              ),
            ),
          ),
        ),
        options = Seq.empty,
        modifiers = None,
        bundleSets = Seq.empty,
        trackInventory = true,
      )
    }
    def randomBundleOption(productId: UUID) = {
      val articleInfo = random[ArticleInfo].copy(id = productId)
      random[BundleOption].copy(id = UUID.randomUUID, article = articleInfo)
    }
  }
}
