package io.paytouch.ordering.utils

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.Uri

import cats.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.{ BundleSet, ImageUrls }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.daos.features.SlickDao
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.entities.{
  CartItemBundleSet,
  GiftCardData,
  GiftCardPassApplied,
  PaymentIntentMetadata,
  PaymentMethod,
}
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus

object FixtureDaoFactory extends FutureHelpers with ConfiguredTestDatabase with Generators { fixtures =>
  private lazy val USD = Currency.getInstance("USD")

  lazy val daos = new Daos

  final case class Wrapper[E <: SlickRecord, U <: SlickUpdate[E]](
      update: U,
      dao: SlickDao {
        type Record = E
        type Update = U
      },
      callback: E => Unit = (e: E) => {},
    ) {
    def create: E = {
      val (_, record) = dao.upsert(update).await
      callback(record)
      record
    }

    def createAndReload: E = {
      val result = create
      dao.findById(result.id).await.get
    }

    // alias to avoid semantic confusion between `val update` and `def create`
    def get = update

    def map[T](f: this.type => T) = f(this)

    def wrapperOnCreated[T <: SlickRecord, Z <: SlickUpdate[T]](f: E => Wrapper[T, Z]) = f(create)
  }

  def merchant(
      id: Option[UUID] = None,
      urlSlug: Option[String] = None,
      paymentProcessor: Option[PaymentProcessor] = Some(PaymentProcessor.Ekashu),
      paymentProcessorConfig: Option[PaymentProcessorConfig] = None,
    ): Wrapper[MerchantRecord, MerchantUpdate] = {
    val _paymentProcessorConfig =
      paymentProcessorConfig.orElse {
        paymentProcessor match {
          case Some(PaymentProcessor.Ekashu) =>
            EkashuConfig(
              "my-ekasu-seller-id",
              "my-ekasu-seller-key",
              "my-ekasu-hash-code",
            ).some

          case Some(PaymentProcessor.Jetdirect) =>
            JetdirectConfig(
              "my-jetdirect-merchant-id",
              "my-jetdirect-terminal-id",
              "my-jetdirect-key",
              "my-jetdirect-code",
            ).some

          case Some(PaymentProcessor.Worldpay) =>
            WorldpayConfig(
              "my-worldpay-account-id",
              "my-worldpay-terminal-id",
              "my-worldpay-acceptor-id",
              "my-worldpay-account-token",
            ).some

          case Some(PaymentProcessor.Stripe) =>
            StripeConfig(
              "my-stripe-account-id",
              "my-stripe-publishable-key",
            ).some

          case _ =>
            PaytouchConfig().some
        }
      }
    val merchant = MerchantUpdate(
      id = id,
      urlSlug = urlSlug.orElse(Some(genString.instance)),
      paymentProcessor = paymentProcessor.orElse(PaymentProcessor.Paytouch.some),
      paymentProcessorConfig = _paymentProcessorConfig,
    )

    Wrapper(merchant, daos.merchantDao)
  }

  def store(
      merchant: MerchantRecord,
      locationId: UUID,
      catalogId: UUID,
      active: Option[Boolean] = None,
      heroImageUrls: Seq[ImageUrls] = Seq.empty,
      logoImageUrls: Seq[ImageUrls] = Seq.empty,
      maxDistance: Option[BigDecimal] = None,
      deliveryMinAmount: Option[BigDecimal] = None,
      deliveryMaxAmount: Option[BigDecimal] = None,
      deliveryFeeAmount: Option[BigDecimal] = None,
      paymentMethods: Option[Seq[PaymentMethod]] = None,
      currency: Option[Currency] = None,
    ) = {

    lazy val defaultPaymentMethods: Seq[PaymentMethod] = Seq(
      PaymentMethod(
        `type` = PaymentMethodType.Cash,
        active = true,
      ),
      PaymentMethod(
        `type` = PaymentMethodType.Ekashu,
        active = true,
      ),
    )

    val store = new StoreUpdate(
      id = None,
      merchantId = Some(merchant.id),
      locationId = Some(locationId),
      currency = currency.orElse(Some(genCurrency.instance)),
      urlSlug = Some(genString.instance),
      catalogId = Some(catalogId),
      active = active.orElse(Some(genBoolean.instance)),
      description = genResettableString.instance,
      heroBgColor = genResettableString.instance,
      heroImageUrls = Some(heroImageUrls),
      logoImageUrls = Some(logoImageUrls),
      takeOutEnabled = Some(genBoolean.instance),
      takeOutStopMinsBeforeClosing = genResettableInt.instance,
      deliveryEnabled = Some(genBoolean.instance),
      deliveryMinAmount = deliveryMinAmount.orElse(Some(genBigDecimal.instance)),
      deliveryMaxAmount = deliveryMaxAmount.orElse(Some(genBigDecimal.instance)),
      deliveryMaxDistance = Some(genBigDecimal.instance),
      deliveryStopMinsBeforeClosing = genResettableInt.instance,
      deliveryFeeAmount = deliveryFeeAmount.orElse(Some(genBigDecimal.instance)),
      paymentMethods = paymentMethods.orElse(Some(defaultPaymentMethods)),
    )
    Wrapper(store, daos.storeDao)
  }

  def cart(
      store: StoreRecord,
      id: Option[UUID] = None,
      orderType: Option[OrderType] = None,
      line1: Option[String] = None,
      city: Option[String] = None,
      postalCode: Option[String] = None,
      orderId: Option[UUID] = None,
      orderNumber: Option[String] = None,
      subtotalAmount: Option[BigDecimal] = None,
      deliveryFeeAmount: Option[BigDecimal] = None,
      taxAmount: Option[BigDecimal] = None,
      tipAmount: Option[BigDecimal] = None,
      totalAmount: Option[BigDecimal] = None,
      totalAmountWithoutGiftCards: Option[BigDecimal] = None,
      paymentMethodType: Option[PaymentMethodType] = None,
      status: Option[CartStatus] = None,
      appliedGiftCardPasses: Option[Seq[GiftCardPassApplied]] = None,
    ) = {
    val address = genAddressUpsertion.instance
    val orderTypeMerge = orderType.getOrElse(genOrderType.instance)
    val storeDefaultDeliveryFeeAmount = orderTypeMerge match {
      case OrderType.Delivery => store.deliveryFeeAmount
      case OrderType.TakeOut  => None
    }

    val logicalStatus =
      if (orderId.isDefined) Some(CartStatus.Paid) else Some(CartStatus.New)

    val logicalPaymentMethodType =
      if (orderId.isDefined) Some(PaymentMethodType.Ekashu) else None

    Wrapper(
      update = new CartUpdate(
        id = id,
        merchantId = store.merchantId.some,
        storeId = store.id.some,
        orderId = orderId,
        orderNumber = orderNumber,
        paymentProcessor = PaymentProcessor.Ekashu.some,
        paymentMethodType = paymentMethodType.orElse(logicalPaymentMethodType),
        currency = store.currency.some,
        subtotalAmount = subtotalAmount.orElse(genBigDecimal.instance.some),
        deliveryFeeAmount = deliveryFeeAmount.orElse(storeDefaultDeliveryFeeAmount),
        taxAmount = taxAmount.orElse(genBigDecimal.instance.some),
        tipAmount = tipAmount.orElse(genBigDecimal.instance.some),
        totalAmountWithoutGiftCards = totalAmountWithoutGiftCards.orElse(genBigDecimal.instance.some),
        totalAmount = totalAmount.orElse(genBigDecimal.instance.some),
        phoneNumber = genResettableString.instance,
        email = genString.instance.some,
        firstName = genString.instance.some,
        lastName = genString.instance.some,
        deliveryAddressLine1 = line1,
        deliveryAddressLine2 = address.line2,
        deliveryCity = city,
        deliveryState = address.state,
        deliveryCountry = address.country,
        deliveryPostalCode = postalCode,
        orderType = orderTypeMerge.some,
        prepareBy = genResettableLocalTime.instance,
        drivingDistanceInMeters = genResettableBigDecimal.instance,
        estimatedDrivingTimeInMins = genResettableInt.instance,
        storeAddress = genAddress.instance.some,
        status = status.orElse(logicalStatus),
        appliedGiftCardPasses = appliedGiftCardPasses,
      ),
      daos.cartDao,
    )
  }

  def cartTaxRate(
      cart: CartRecord,
      value: Option[BigDecimal] = None,
      taxRateId: Option[UUID] = None,
    ) = {
    val cartTaxRate = new CartTaxRateUpdate(
      id = None,
      storeId = Some(cart.storeId),
      cartId = Some(cart.id),
      taxRateId = taxRateId.orElse(Some(genUUID.instance)),
      name = Some(genString.instance),
      `value` = value.orElse(Some(genBigDecimal.instance)),
      totalAmount = Some(genBigDecimal.instance),
    )
    Wrapper(cartTaxRate, daos.cartTaxRateDao)
  }

  def cartItem(
      cart: CartRecord,
      productId: Option[UUID] = None,
      quantity: Option[BigDecimal] = None,
      priceAmount: Option[BigDecimal] = None,
      costAmount: Option[BigDecimal] = None,
      calculatedPriceAmount: Option[BigDecimal] = None,
      overrideNow: Option[ZonedDateTime] = None,
      bundleSets: Option[Seq[CartItemBundleSet]] = None,
      `type`: Option[CartItemType] = None,
      giftCardData: Option[GiftCardData] = None,
    ): Wrapper[CartItemRecord, CartItemUpdate] =
    Wrapper(
      dao = daos.cartItemDao,
      update = new CartItemUpdate(
        id = None,
        storeId = cart.storeId.some,
        cartId = cart.id.some,
        productId = productId.orElse(genUUID.sample),
        productName = genString.sample,
        productDescription = genString.sample,
        quantity = quantity.orElse(genBigDecimal.sample),
        unit = genUnitType.sample,
        priceAmount = priceAmount.orElse(genBigDecimal.sample),
        costAmount = costAmount.orElse(genBigDecimal.sample),
        taxAmount = genBigDecimal.sample,
        calculatedPriceAmount = calculatedPriceAmount.orElse(genBigDecimal.sample),
        totalPriceAmount = genBigDecimal.sample,
        notes = genString.sample,
        bundleSets = bundleSets,
        `type` = `type`.orElse(CartItemType.Product.some),
        giftCardData = giftCardData,
      ) {
        override def now = overrideNow.getOrElse(UtcTime.now)
      },
    )

  def cartItemGiftCard(
      cart: CartRecord,
      productId: Option[UUID] = None,
      quantity: Option[BigDecimal] = None,
      priceAmount: Option[BigDecimal] = None,
      calculatedPriceAmount: Option[BigDecimal] = None,
      giftCardData: Option[GiftCardData] = None,
    ): Wrapper[CartItemRecord, CartItemUpdate] =
    cartItem(
      cart,
      priceAmount = priceAmount,
      calculatedPriceAmount = calculatedPriceAmount,
      giftCardData = genGiftCardData.instance.some,
      `type` = CartItemType.GiftCard.some,
    )

  def cartItemModifierOption(cartItem: CartItemRecord, modifierOptionId: Option[UUID] = None) = {
    val cartItemModifierOption = new CartItemModifierOptionUpdate(
      id = None,
      storeId = Some(cartItem.storeId),
      cartItemId = Some(cartItem.id),
      modifierOptionId = modifierOptionId.orElse(Some(genUUID.instance)),
      name = Some(genString.instance),
      `type` = Some(genModifierSetType.instance),
      priceAmount = Some(genBigDecimal.instance),
      quantity = Some(genBigDecimal.instance),
    )
    Wrapper(cartItemModifierOption, daos.cartItemModifierOptionDao)
  }

  def cartItemTaxRate(
      cartItem: CartItemRecord,
      value: Option[BigDecimal] = None,
      taxRateId: Option[UUID] = None,
    ) = {
    val cartItemTaxRate = new CartItemTaxRateUpdate(
      id = None,
      storeId = Some(cartItem.storeId),
      cartItemId = Some(cartItem.id),
      taxRateId = taxRateId.orElse(Some(genUUID.instance)),
      name = Some(genString.instance),
      `value` = value.orElse(Some(genBigDecimal.instance)),
      totalAmount = Some(genBigDecimal.instance),
      applyToPrice = Some(genBoolean.instance),
    )
    Wrapper(cartItemTaxRate, daos.cartItemTaxRateDao)
  }

  def cartItemVariantOption(cartItem: CartItemRecord, variantOptionId: Option[UUID] = None) = {
    val cartItemVariantOption = new CartItemVariantOptionUpdate(
      id = None,
      storeId = Some(cartItem.storeId),
      cartItemId = Some(cartItem.id),
      variantOptionId = variantOptionId.orElse(Some(genUUID.instance)),
      optionName = Some(genString.instance),
      optionTypeName = Some(genString.instance),
    )
    Wrapper(cartItemVariantOption, daos.cartItemVariantOptionDao)
  }

  def worldpayPayment(
      cart: CartRecord,
      successReturnUrl: Uri,
      failureReturnUrl: Uri,
    ) = {
    val worldpayPayment = new WorldpayPaymentUpdate(
      id = None,
      objectId = Some(cart.id),
      objectType = Some(WorldpayPaymentType.Cart),
      transactionSetupId = Some(genString.instance),
      successReturnUrl = Some(successReturnUrl),
      failureReturnUrl = Some(failureReturnUrl),
      status = Some(WorldpayPaymentStatus.Submitted),
    )
    Wrapper(worldpayPayment, daos.worldpayPaymentDao)
  }

  def worldpayPayment(
      paymentIntent: PaymentIntentRecord,
      successReturnUrl: Uri,
      failureReturnUrl: Uri,
    ) = {
    val worldpayPayment = new WorldpayPaymentUpdate(
      id = None,
      objectId = Some(paymentIntent.id),
      objectType = Some(WorldpayPaymentType.PaymentIntent),
      transactionSetupId = Some(genString.instance),
      successReturnUrl = Some(successReturnUrl),
      failureReturnUrl = Some(failureReturnUrl),
      status = Some(WorldpayPaymentStatus.Submitted),
    )
    Wrapper(worldpayPayment, daos.worldpayPaymentDao)
  }

  def paymentIntent(
      merchant: MerchantRecord,
      orderId: UUID,
      status: Option[PaymentIntentStatus],
    ) = {
    val paymentIntent = new PaymentIntentUpdate(
      id = None,
      merchantId = Some(merchant.id),
      orderId = Some(orderId),
      orderItemIds = Some(Seq(UUID.randomUUID)),
      subtotalAmount = Some(genBigDecimal.instance),
      taxAmount = Some(genBigDecimal.instance),
      tipAmount = Some(genBigDecimal.instance),
      totalAmount = Some(genBigDecimal.instance),
      paymentMethodType = Some(PaymentMethodType.Worldpay),
      successReturnUrl = Some(genString.instance),
      failureReturnUrl = Some(genString.instance),
      status = status,
      metadata = Some(PaymentIntentMetadata.empty),
    )
    Wrapper(paymentIntent, daos.paymentIntentDao)
  }
}
