package io.paytouch.ordering.services

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.conversions.OrderConversions
import io.paytouch.ordering.data.model.StoreRecord
import io.paytouch.ordering.entities.{ DeliveryAddressUpsertion => _, _ }
import io.paytouch.ordering.utils.{
  CommonArbitraries,
  DaoSpec,
  DefaultFixtures,
  MockedRestApi,
  OrdersGetFixture,
  UUIDConversion,
  UtcTime,
  FixtureDaoFactory => Factory,
}
import org.specs2.specification.Scope
import org.scalacheck.Arbitrary

@scala.annotation.nowarn("msg=Auto-application")
class OrderConversionSpec extends DaoSpec with CommonArbitraries {
  class OrderConversionSpecContext extends DefaultFixtures with Scope with OrderConversions with UUIDConversion {
    val service = MockedRestApi.cartSyncService

    implicit val storeContext = StoreContext.fromRecord(londonStore)

    lazy val product1 = random[Product].copy(
      id = "d35fff4a-d7d6-44e7-81a6-fa532a94eda9",
      name = "Fresh Brunch",
    )

    lazy val bundledProduct1 = random[Product].copy(
      id = "6d387088-d0bc-4bb1-8014-cfbe4ebc5044",
      name = "Greek Bowl",
    )

    lazy val bundledProduct2 = random[Product].copy(
      id = "6fccd80b-3252-4cda-a613-bbbdbfe66ef1",
      name = "California Salad",
    )

    lazy val cartItemBundleSet1 = CartItemBundleSet(
      bundleSetId = "4557fd88-2d18-413b-acea-7a6f28e8c099",
      name = Some("Choose Bowl"),
      position = 1,
      cartItemBundleOptions = Seq(cartItemBundleOption1),
    )

    lazy val cartItemBundleOption1 = CartItemBundleOption(
      bundleOptionId = "2e149e5b-5cef-4b40-bdea-e1989a4bca41",
      item = CartItemBundleOptionItem(
        product = CartItemProduct(
          id = bundledProduct1.id,
          name = bundledProduct1.name,
          description = bundledProduct1.description,
        ),
        quantity = 2,
        unit = genUnitType.instance,
        cost = None,
        notes = None,
        modifierOptions = Seq.empty,
        variantOptions = Seq.empty,
      ),
      priceAdjustment = 0,
      position = 1,
    )

    lazy val cartItemBundleSet2 = CartItemBundleSet(
      bundleSetId = "9e2db18c-8d5a-4c04-bf70-90d47cdc0e75",
      name = Some("Choose Salad"),
      position = 2,
      cartItemBundleOptions = Seq(cartItemBundleOption2),
    )

    lazy val cartItemBundleOption2 = CartItemBundleOption(
      bundleOptionId = "137e9b82-1e3b-4ea1-8696-379d91df3863",
      item = CartItemBundleOptionItem(
        product = CartItemProduct(
          id = bundledProduct2.id,
          name = bundledProduct2.name,
          description = bundledProduct2.description,
        ),
        quantity = 1,
        unit = genUnitType.instance,
        cost = None,
        notes = None,
        modifierOptions = Seq.empty,
        variantOptions = Seq.empty,
      ),
      priceAdjustment = 0,
      position = 1,
    )

    val coreItemTaxRateId = UUID.randomUUID
    val coreTaxRateId = UUID.randomUUID
    val coreModifierOptionId = UUID.randomUUID
    val coreVariantOptionId = UUID.randomUUID

    val baseExpectedOrderUpsertion = OrderUpsertion(
      locationId = genUUID.instance,
      `type` = OrderType.DeliveryRestaurant,
      paymentType = None,
      totalAmount = BigDecimal(0),
      subtotalAmount = BigDecimal(0),
      taxAmount = BigDecimal(0),
      tipAmount = None,
      deliveryFeeAmount = None,
      paymentStatus = PaymentStatus.Pending,
      source = OrderSource.Storefront,
      status = OrderStatus.Received,
      isInvoice = false,
      paymentTransactions = Seq.empty,
      items = Seq.empty,
      deliveryAddress = random[DeliveryAddressUpsertion],
      onlineOrderAttribute = random[OnlineOrderAttributeUpsertion],
      receivedAt = UtcTime.now,
      completedAt = None,
      taxRates = Seq.empty,
      bundles = Seq.empty,
    )

    def assertOrderUpsertion(
        order: OrderUpsertion,
        cart: Cart,
        store: StoreRecord,
        existingOrder: Option[Order],
        expectedOrderUpsertion: OrderUpsertion,
      ) = {
      order.locationId ==== store.locationId
      order.`type` ==== cart.orderType.coreOrderType
      order.paymentType ==== None
      order.totalAmount ==== cart.total.amount
      order.subtotalAmount ==== cart.subtotal.amount
      order.taxAmount ==== cart.tax.amount
      order.tipAmount ==== Some(cart.tip.amount)
      order.deliveryFeeAmount ==== cart.deliveryFee.map(_.amount)
      order.paymentStatus ==== expectedOrderUpsertion.paymentStatus
      order.source ==== expectedOrderUpsertion.source
      order.status ==== expectedOrderUpsertion.status
      order.isInvoice ==== expectedOrderUpsertion.isInvoice
      order.completedAt.isDefined ==== expectedOrderUpsertion.completedAt.isDefined
      order.paymentTransactions ==== Seq.empty

      assertOrderDeliveryAddress(order.deliveryAddress, cart)
      assertOnlineOrderAttribute(order.onlineOrderAttribute, cart)

      order.taxRates.size ==== cart.taxRates.size
      order.taxRates.zip(cart.taxRates).map {
        case (orderTaxRate, cartTaxRate) =>
          assertOrderTaxRate(orderTaxRate, cartTaxRate)
      }

      assertBundles(order, cart, existingOrder)

      val bundledItemsSize =
        cart
          .items
          .map(item =>
            item
              .bundleSets
              .getOrElse(Seq.empty)
              .map(set => set.cartItemBundleOptions.size)
              .sum,
          )
          .sum
      order.items.size ==== cart.items.size + bundledItemsSize

      cart.items.map { cartItem =>
        val orderItem = order.items.find(i => i.id === cartItem.id)
        orderItem.isDefined ==== true
        assertOrderItem(orderItem.get, cartItem)
      }
    }

    private def assertOrderDeliveryAddress(orderDeliveryAddress: DeliveryAddressUpsertion, cart: Cart) = {
      orderDeliveryAddress.firstName ==== cart.deliveryAddress.firstName
      orderDeliveryAddress.lastName ==== cart.deliveryAddress.lastName
      orderDeliveryAddress.address.line1 ==== cart.deliveryAddress.address.line1
      orderDeliveryAddress.address.line2 ==== cart.deliveryAddress.address.line2
      orderDeliveryAddress.address.city ==== cart.deliveryAddress.address.city
      orderDeliveryAddress.address.state ==== cart.deliveryAddress.address.state
      orderDeliveryAddress.address.country ==== cart.deliveryAddress.address.country
      orderDeliveryAddress.address.postalCode ==== cart.deliveryAddress.address.postalCode
      orderDeliveryAddress.drivingDistanceInMeters ==== cart.drivingDistanceInMeters
      orderDeliveryAddress.estimatedDrivingTimeInMins ==== cart.estimatedDrivingTimeInMins
    }

    private def assertOnlineOrderAttribute(onlineAttribute: OnlineOrderAttributeUpsertion, cart: Cart) = {
      onlineAttribute.email ==== cart.email
      onlineAttribute.firstName ==== cart.deliveryAddress.firstName
      onlineAttribute.lastName ==== cart.deliveryAddress.lastName
      onlineAttribute.phoneNumber ==== cart.phoneNumber
      onlineAttribute.prepareByTime ==== cart.prepareBy
      onlineAttribute.acceptanceStatus ==== AcceptanceStatus.Open
    }

    private def assertOrderTaxRate(orderTaxRate: OrderTaxRateUpsertion, cartTaxRate: CartTaxRate) = {
      orderTaxRate.taxRateId ==== coreTaxRateId
      orderTaxRate.name ==== cartTaxRate.name
      orderTaxRate.value ==== cartTaxRate.`value`
      orderTaxRate.totalAmount ==== cartTaxRate.total.amount
    }

    private def assertOrderItem(orderItem: OrderItemUpsertion, cartItem: CartItem) = {
      orderItem.id ==== cartItem.id
      orderItem.productId ==== Some(cartItem.product.id)
      orderItem.productName ==== Some(cartItem.product.name)
      orderItem.productDescription ==== cartItem.product.description
      orderItem.quantity ==== Some(cartItem.quantity)
      orderItem.unit ==== Some(cartItem.unit)
      orderItem.paymentStatus ==== Some(PaymentStatus.Pending)
      orderItem.priceAmount ==== Some(cartItem.price.amount)
      orderItem.costAmount ==== cartItem.cost.map(_.amount)
      orderItem.taxAmount ==== Some(cartItem.tax.amount)
      orderItem.discountAmount ==== None
      orderItem.calculatedPriceAmount ==== Some(cartItem.calculatedPrice.amount)
      orderItem.totalPriceAmount ==== Some(cartItem.totalPrice.amount)
      orderItem.notes ==== cartItem.notes

      orderItem.modifierOptions.size ==== cartItem.modifierOptions.size
      orderItem.modifierOptions.zip(cartItem.modifierOptions).map {
        case (orderModifierOption, cartModifierOption) =>
          assertOrderItemModifierOption(orderModifierOption, cartModifierOption)
      }

      orderItem.variantOptions.size ==== cartItem.variantOptions.size
      orderItem.variantOptions.zip(cartItem.variantOptions).map {
        case (orderVariantOption, cartVariantOption) =>
          assertOrderItemVariantOption(orderVariantOption, cartVariantOption)
      }

      orderItem.taxRates.size ==== cartItem.taxRates.size
      orderItem.taxRates.zip(cartItem.taxRates).map {
        case (orderTaxRate, cartTaxRate) =>
          assertOrderItemTaxRate(orderTaxRate, cartTaxRate)
      }
    }

    private def assertOrderItemModifierOption(
        orderItemModifierOption: OrderItemModifierOptionUpsertion,
        cartItemModifierOption: CartItemModifierOption,
      ) = {
      orderItemModifierOption.modifierOptionId ==== Some(coreModifierOptionId)
      orderItemModifierOption.name ==== cartItemModifierOption.name
      orderItemModifierOption.`type` ==== cartItemModifierOption.`type`
      orderItemModifierOption.price ==== cartItemModifierOption.price.amount
      orderItemModifierOption.quantity ==== cartItemModifierOption.quantity
    }

    private def assertOrderItemVariantOption(
        orderItemVariantOption: OrderItemVariantOptionUpsertion,
        cartItemVariantOption: CartItemVariantOption,
      ) = {
      orderItemVariantOption.variantOptionId ==== Some(coreVariantOptionId)
      orderItemVariantOption.optionName ==== Some(cartItemVariantOption.optionName)
      orderItemVariantOption.optionTypeName ==== Some(cartItemVariantOption.optionTypeName)
    }

    private def assertOrderItemTaxRate(
        orderItemTaxRate: OrderItemTaxRateUpsertion,
        cartItemTaxRate: CartItemTaxRate,
      ) = {
      orderItemTaxRate.taxRateId ==== Some(coreItemTaxRateId)
      orderItemTaxRate.name ==== cartItemTaxRate.name
      orderItemTaxRate.value ==== cartItemTaxRate.`value`
      orderItemTaxRate.totalAmount ==== Some(cartItemTaxRate.total.amount)
      orderItemTaxRate.applyToPrice ==== cartItemTaxRate.applyToPrice
    }

    private def assertBundles(
        order: OrderUpsertion,
        cart: Cart,
        existingOrder: Option[Order],
      ) = {
      val cartBundleItems = cart.items.find(i => i.bundleSets.isDefined)
      order.bundles.size ==== cartBundleItems.size

      cartBundleItems.map { bundleItem =>
        val orderBundle =
          order.bundles.find(b => b.bundleOrderItemId == bundleItem.id).get
        orderBundle.orderBundleSets.size ==== bundleItem.bundleSets.get.size

        val existingOrderBundle = existingOrder.flatMap(_.bundles.find(_.bundleOrderItemId == bundleItem.id))
        if (existingOrder.isDefined) {
          existingOrderBundle.isDefined === true
          existingOrderBundle.map(_.id === orderBundle.id)
        }

        orderBundle.orderBundleSets.zip(bundleItem.bundleSets.get).map {
          case (orderBundleSet, cartBundleSet) =>
            assertOrderBundleSet(orderBundleSet, cartBundleSet, order.items, existingOrderBundle)
        }
      }
    }

    private def assertOrderBundleSet(
        orderBundleSet: OrderBundleSetUpsertion,
        cartBundleSet: CartItemBundleSet,
        orderItems: Seq[OrderItemUpsertion],
        existingOrderBundle: Option[OrderBundle],
      ) = {
      val existingBundleSet = existingOrderBundle.flatMap(
        _.orderBundleSets.find(set =>
          set
            .bundleSetId
            .contains(cartBundleSet.bundleSetId) && set.position == cartBundleSet.position,
        ),
      )

      existingBundleSet.map(_.id === orderBundleSet.id)

      orderBundleSet.bundleSetId === cartBundleSet.bundleSetId
      orderBundleSet.name === cartBundleSet.name
      orderBundleSet.position === cartBundleSet.position

      orderBundleSet
        .orderBundleOptions
        .zip(cartBundleSet.cartItemBundleOptions)
        .map {
          case (orderBundleOption, cartBundleOption) =>
            assertOrderBundleOption(orderBundleOption, cartBundleOption, orderItems, existingBundleSet)
        }
    }

    private def assertOrderBundleOption(
        orderBundleOption: OrderBundleOptionUpsertion,
        cartBundleOption: CartItemBundleOption,
        orderItems: Seq[OrderItemUpsertion],
        existingBundleSet: Option[OrderBundleSet],
      ) = {
      val existingBundleOption = existingBundleSet.flatMap(
        _.orderBundleOptions.find(option =>
          option
            .bundleOptionId
            .contains(cartBundleOption.bundleOptionId) && option.position == cartBundleOption.position,
        ),
      )

      existingBundleOption.map { existing =>
        orderBundleOption.id === existing.id
        Some(orderBundleOption.articleOrderItemId) === existing.articleOrderItemId
      }

      orderBundleOption.bundleOptionId === cartBundleOption.bundleOptionId
      orderBundleOption.position === cartBundleOption.position

      val orderItem = orderItems
        .find(item => orderBundleOption.articleOrderItemId == item.id)
        .get
      orderItem.productId === Some(cartBundleOption.item.product.id)
      orderItem.quantity === Some(cartBundleOption.item.quantity)
    }
  }

  "should convert complete cart to order" in new OrderConversionSpecContext {
    val cartRecord = Factory.cart(londonStore).create
    Factory.cartTaxRate(cartRecord, taxRateId = Some(coreTaxRateId)).create
    val cartItem = Factory.cartItem(cartRecord).create
    Factory
      .cartItemTaxRate(cartItem, taxRateId = Some(coreItemTaxRateId))
      .create
    Factory
      .cartItemModifierOption(cartItem, modifierOptionId = Some(coreModifierOptionId))
      .create
    Factory.cartItemVariantOption(cartItem, variantOptionId = Some(coreVariantOptionId))

    val cartEntity = MockedRestApi.cartService.enrich(cartRecord).await

    val orderUpsertion = toOrderUpsertion(cartEntity, None)

    assertOrderUpsertion(orderUpsertion, cartEntity, londonStore, None, baseExpectedOrderUpsertion)
  }

  "should convert complete cart to complete order if it contains gift cards only" in new OrderConversionSpecContext {
    val cartRecord = Factory.cart(londonStore).create
    val cartItem = Factory.cartItemGiftCard(cartRecord).create
    val cartEntity = MockedRestApi.cartService.enrich(cartRecord).await

    val orderUpsertion = toOrderUpsertion(cartEntity, None)

    assertOrderUpsertion(
      orderUpsertion,
      cartEntity,
      londonStore,
      None,
      baseExpectedOrderUpsertion.copy(status = OrderStatus.Completed, completedAt = Some(UtcTime.now)),
    )
  }

  "should convert complete cart with bundles to order" in new OrderConversionSpecContext {
    val cartRecord = Factory.cart(londonStore).create
    Factory.cartTaxRate(cartRecord, taxRateId = Some(coreTaxRateId)).create
    val cartItem =
      Factory
        .cartItem(cartRecord, bundleSets = Some(Seq(cartItemBundleSet1, cartItemBundleSet2)))
        .create
    Factory
      .cartItemTaxRate(cartItem, taxRateId = Some(coreItemTaxRateId))
      .create

    val cartEntity = MockedRestApi.cartService.enrich(cartRecord).await

    val orderUpsertion = toOrderUpsertion(cartEntity, None)

    assertOrderUpsertion(orderUpsertion, cartEntity, londonStore, None, baseExpectedOrderUpsertion)
  }

  "should convert complete cart with bundles to order with existing order" in new OrderConversionSpecContext
    with OrdersGetFixture {
    val cartRecord = Factory.cart(londonStore).create
    Factory.cartTaxRate(cartRecord, taxRateId = Some(coreTaxRateId)).create
    val cartItem =
      Factory
        .cartItem(cartRecord, bundleSets = Some(Seq(cartItemBundleSet1, cartItemBundleSet2)))
        .create
    Factory
      .cartItemTaxRate(cartItem, taxRateId = Some(coreItemTaxRateId))
      .create

    val cartEntity = MockedRestApi.cartService.enrich(cartRecord).await

    val existingOrder = ordersGetFixture.copy(
      items = Seq(
        OrderItem(
          id = cartItem.id,
          productId = Some(product1.id),
          productName = Some("Fresh Brunch"),
          quantity = Some(1),
          paymentStatus = Some(PaymentStatus.Pending),
          calculatedPrice = Some(MonetaryAmount(0, USD)),
          tax = Some(MonetaryAmount(0, USD)),
          totalPrice = Some(MonetaryAmount(0, USD)),
          variantOptions = Seq.empty,
          modifierOptions = Seq.empty,
          taxRates = Seq.empty,
        ),
        OrderItem(
          id = "476819b2-7568-49fa-9dd0-b51e519d82cc",
          productId = Some(bundledProduct1.id),
          productName = Some("Greek Bowl"),
          quantity = Some(1),
          paymentStatus = Some(PaymentStatus.Pending),
          calculatedPrice = Some(MonetaryAmount(0, USD)),
          tax = Some(MonetaryAmount(0, USD)),
          totalPrice = Some(MonetaryAmount(0, USD)),
          variantOptions = Seq.empty,
          modifierOptions = Seq.empty,
          taxRates = Seq.empty,
        ),
        OrderItem(
          id = "87f2c301-75c5-47d8-aead-f9b6632ce452",
          productId = Some(bundledProduct2.id),
          productName = Some("California Salad"),
          quantity = Some(1),
          paymentStatus = Some(PaymentStatus.Pending),
          calculatedPrice = Some(MonetaryAmount(0, USD)),
          tax = Some(MonetaryAmount(0, USD)),
          totalPrice = Some(MonetaryAmount(0, USD)),
          variantOptions = Seq.empty,
          modifierOptions = Seq.empty,
          taxRates = Seq.empty,
        ),
      ),
      bundles = Seq(
        OrderBundle(
          id = "3bb521ba-bda0-4d66-af1f-98c967be8757",
          bundleOrderItemId = cartItem.id,
          orderBundleSets = Seq(
            OrderBundleSet(
              id = "c975c60c-a7e6-4b10-97d3-d2d63e079001",
              bundleSetId = Some("4557fd88-2d18-413b-acea-7a6f28e8c099"),
              name = Some("Choose Bowl"),
              position = 1,
              orderBundleOptions = Seq(
                OrderBundleOption(
                  id = "aecae035-875a-4817-93f1-9c1230779e3f",
                  bundleOptionId = Some("2e149e5b-5cef-4b40-bdea-e1989a4bca41"),
                  articleOrderItemId = Some("476819b2-7568-49fa-9dd0-b51e519d82cc"),
                  position = 1,
                  priceAdjustment = 0,
                ),
              ),
            ),
            OrderBundleSet(
              id = "809b15d1-8d0b-43a8-96a8-411b5af23037",
              bundleSetId = Some("9e2db18c-8d5a-4c04-bf70-90d47cdc0e75"),
              name = Some("Choose Salad"),
              position = 2,
              orderBundleOptions = Seq(
                OrderBundleOption(
                  id = "be92f8dd-b792-427e-ae30-3a1c51361e3d",
                  bundleOptionId = Some("137e9b82-1e3b-4ea1-8696-379d91df3863"),
                  articleOrderItemId = Some("87f2c301-75c5-47d8-aead-f9b6632ce452"),
                  position = 1,
                  priceAdjustment = 0,
                ),
              ),
            ),
          ),
        ),
      ),
    )

    val orderUpsertion = toOrderUpsertion(cartEntity, Some(existingOrder))

    existingOrder.items.map(_.id) ==== orderUpsertion.items.map(_.id)
    assertOrderUpsertion(orderUpsertion, cartEntity, londonStore, Some(existingOrder), baseExpectedOrderUpsertion)
  }
}
