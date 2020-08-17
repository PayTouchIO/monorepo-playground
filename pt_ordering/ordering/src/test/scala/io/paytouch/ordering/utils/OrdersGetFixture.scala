package io.paytouch.ordering.utils

import java.util.{ Currency, UUID }
import java.time.{ ZoneId, ZonedDateTime }

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.entities.enums.ModifierSetType
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.utils.Formatters.ZonedDateTimeFormatter

trait OrdersGetFixture extends UUIDConversion {
  private lazy val USD = Currency.getInstance("USD")

  lazy val ordersGetFixture = Order(
    id = "ac544e46-63a7-4d2f-81f9-53f68bcd02b2",
    location = Some(
      Location(
        id = "fdd8866e-b609-44af-97b2-3a6d0582d78c",
        name = "Fresh Natural Food - Central Drive",
        email = Some("ordering.tommy-block+all@example.com"),
        phoneNumber = "",
        website = None,
        active = true,
        address = Address(
          line1 = "2200 Central Drive".some,
          line2 = None,
          city = "Bedford".some,
          state = "Texas".some,
          country = "United States of America".some,
          postalCode = "76021".some,
          stateData = State(
            State.Name("Texas").some,
            State.Code("TX"),
            Country(
              Country.Code("US"),
              Country.Name("United States"),
            ).some,
          ).some,
        ),
        timezone = ZoneId.of("America/Los_Angeles"),
        currency = Currency.getInstance("USD"),
        settings = None,
        openingHours = None,
        coordinates = Some(
          Coordinates(
            lat = 32.84147180000,
            lng = -97.13301420000,
          ),
        ),
      ),
    ),
    number = Some("12"),
    status = OrderStatus.Received,
    paymentStatus = PaymentStatus.Paid,
    items = Seq(
      OrderItem(
        id = "41c806cb-15ea-4d82-b95c-b197caed42d7",
        productId = Some("d35fff4a-d7d6-44e7-81a6-fa532a94eda9"),
        productName = Some("Fresh Brunch"),
        calculatedPrice = Some(MonetaryAmount(16, USD)),
        tax = Some(MonetaryAmount(0, USD)),
        totalPrice = Some(MonetaryAmount(16.5, USD)),
        quantity = Some(1),
        paymentStatus = Some(PaymentStatus.Paid),
        taxRates = Seq.empty,
        variantOptions = Seq.empty,
        modifierOptions = Seq.empty,
      ),
      OrderItem(
        id = "94271ea8-8808-407a-b681-22c119eb0b9e",
        productId = Some("15e392ac-5f5a-4108-96e7-7ca18e9738c4"),
        productName = Some("Just Greens Smoothie"),
        quantity = Some(1),
        paymentStatus = Some(PaymentStatus.Paid),
        calculatedPrice = Some(MonetaryAmount(5.99, USD)),
        tax = Some(MonetaryAmount(0.52, USD)),
        totalPrice = Some(MonetaryAmount(6.51, USD)),
        taxRates = Seq(
          OrderItemTaxRate(
            id = "5791d1ca-5733-468f-9ec6-9f7bd33332ef",
            taxRateId = Some("d01f2299-5941-4d3e-a19c-f9986f058973"),
            name = "City Tax",
            value = 8.740,
            totalAmount = Some(0.52),
            applyToPrice = true,
            active = true,
          ),
        ),
        variantOptions = Seq(
          OrderItemVariantOption(
            optionName = "Small",
            optionTypeName = "Size",
            position = 0,
          ),
        ),
        modifierOptions = Seq.empty,
      ),
      OrderItem(
        id = "476819b2-7568-49fa-9dd0-b51e519d82cc",
        productId = Some("6d387088-d0bc-4bb1-8014-cfbe4ebc5044"),
        productName = Some("Greek Bowl"),
        quantity = Some(1),
        paymentStatus = Some(PaymentStatus.Paid),
        calculatedPrice = Some(MonetaryAmount(0, USD)),
        tax = Some(MonetaryAmount(0, USD)),
        totalPrice = None,
        taxRates = Seq.empty,
        variantOptions = Seq.empty,
        modifierOptions = Seq(
          OrderItemModifierOption(
            name = "Greek Lime",
            modifierSetName = Some("Dressing"),
            `type` = ModifierSetType.Addon,
            price = MonetaryAmount(0.5, USD),
            quantity = 1,
          ),
          OrderItemModifierOption(
            name = "Salt",
            modifierSetName = Some("Seasonings"),
            `type` = ModifierSetType.Hold,
            price = MonetaryAmount(0, USD),
            quantity = 1,
          ),
        ),
      ),
      OrderItem(
        id = "87f2c301-75c5-47d8-aead-f9b6632ce452",
        productId = Some("6fccd80b-3252-4cda-a613-bbbdbfe66ef1"),
        productName = Some("California Salad"),
        quantity = Some(1),
        paymentStatus = Some(PaymentStatus.Paid),
        calculatedPrice = Some(MonetaryAmount(0, USD)),
        tax = Some(MonetaryAmount(0, USD)),
        totalPrice = None,
        taxRates = Seq.empty,
        variantOptions = Seq.empty,
        modifierOptions = Seq(
          OrderItemModifierOption(
            name = "Yogurt",
            modifierSetName = Some("Dressing"),
            `type` = ModifierSetType.Addon,
            price = MonetaryAmount(0, USD),
            quantity = 1,
          ),
          OrderItemModifierOption(
            name = "Pepper",
            modifierSetName = Some("Seasonings"),
            `type` = ModifierSetType.Hold,
            price = MonetaryAmount(0, USD),
            quantity = 1,
          ),
        ),
      ),
      OrderItem(
        id = "82820abb-0fdd-4b9f-a33b-8fae0cc1e70d",
        productId = Some("2ba749ba-b35c-4ceb-bb97-319a5ca53e27"),
        productName = Some("Very Berry Smoothie"),
        quantity = Some(1),
        paymentStatus = Some(PaymentStatus.Paid),
        calculatedPrice = Some(MonetaryAmount(0, USD)),
        tax = Some(MonetaryAmount(0, USD)),
        totalPrice = None,
        taxRates = Seq.empty,
        variantOptions = Seq(OrderItemVariantOption(optionName = "Small", optionTypeName = "Size", position = 0)),
        modifierOptions = Seq.empty,
      ),
    ),
    bundles = Seq(
      OrderBundle(
        id = "3bb521ba-bda0-4d66-af1f-98c967be8757",
        bundleOrderItemId = "41c806cb-15ea-4d82-b95c-b197caed42d7",
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
          OrderBundleSet(
            id = "281f4710-251f-4b0c-bb30-fb57e59e34e7",
            bundleSetId = Some("b53daa23-9c61-41a6-9333-707dc148b3ab"),
            name = Some("Choose Smoothie"),
            position = 3,
            orderBundleOptions = Seq(
              OrderBundleOption(
                id = "ca59461a-ef15-4240-8abd-830d6a76d359",
                bundleOptionId = Some("59dd4f60-3363-4e97-af78-e511a15ae359"),
                articleOrderItemId = Some("82820abb-0fdd-4b9f-a33b-8fae0cc1e70d"),
                position = 1,
                priceAdjustment = 0,
              ),
            ),
          ),
        ),
      ),
    ),
    taxRates = Seq(
      OrderTaxRate(
        id = "42c4da40-fab3-4307-aa44-1c604fc5e240",
        taxRateId = Some("d01f2299-5941-4d3e-a19c-f9986f058973"),
        name = "City Tax",
        value = 8.740,
        totalAmount = 0.52,
      ),
    ),
    paymentTransactions = Seq.empty,
    subtotal = Some(MonetaryAmount(22.49, USD)),
    tax = Some(MonetaryAmount(0.52, USD)),
    tip = Some(MonetaryAmount(0, USD)),
    total = Some(MonetaryAmount(23.01, USD)),
    onlineOrderAttribute = OnlineOrderAttribute(
      id = "3f615ffa-aa65-4382-9791-099882e9b947",
      acceptanceStatus = AcceptanceStatus.Pending,
      rejectionReason = None,
      prepareByTime = None,
      prepareByDateTime = None,
      estimatedPrepTimeInMins = None,
      rejectedAt = None,
      acceptedAt = None,
      estimatedReadyAt = None,
      estimatedDeliveredAt = None,
      cancellationReason = None,
    ).some,
    completedAt = None,
    createdAt = ZonedDateTime.parse("2019-11-12T10:02:42.857Z"),
    updatedAt = ZonedDateTime.parse("2019-11-12T10:03:23.355Z"),
  )
}
