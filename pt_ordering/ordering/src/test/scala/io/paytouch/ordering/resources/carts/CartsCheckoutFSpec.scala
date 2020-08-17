package io.paytouch.ordering.resources.carts

import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.{ CoreEmbeddedError, CoreEmbeddedErrorResponse }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.clients.paytouch.core.entities.OnlineOrderAttribute
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.data.model.{ StripeConfig => StripePaymentProcessorConfig }
import io.paytouch.ordering.data.model.WorldpayPaymentType
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType, PaymentMethodType, PaymentProcessor }
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import io.paytouch.ordering.stubs.{ GMapsStubData, PtCoreStubData }
import io.paytouch.ordering.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CartsCheckoutFSpec extends CartsFSpec {
  abstract class CartsCheckoutFSpecContext extends CartResourceFSpecContext {
    override lazy val londonDeliveryMinAmount = Some(0)
    override lazy val londonDeliveryMaxAmount = Some(100)
    override lazy val londonDeliveryFeeAmount = Some(20)

    val orderType: OrderType
    val line1: Option[String] = None
    val city: Option[String] = None
    val postalCode: Option[String] = None
    val initialTipAmount: Option[BigDecimal] = Some(5)
    lazy val cart =
      Factory
        .cart(
          londonStore,
          orderType = Some(orderType),
          line1 = line1,
          city = city,
          postalCode = postalCode,
          tipAmount = initialTipAmount,
        )
        .create
    lazy val id = cart.id

    implicit val storeContext = StoreContext.fromRecord(londonStore)
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    lazy val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.Open

    lazy val order =
      randomOrder(acceptanceStatus.some)

    PtCoreStubData.recordOrder(order)
  }

  "POST /v1/carts.checkout?cart_id=<cart-id>" in {
    "update and checkout" in {
      abstract class UpdateAndCheckoutContext extends CartsCheckoutFSpecContext {
        lazy val updateEmail: ResettableString = None
        lazy val updatePhoneNumber: ResettableString = None
        lazy val updatePrepareBy: ResettableLocalTime = None
        lazy val updateOrderType: Option[OrderType] = None
        lazy val updateTipAmount: Option[BigDecimal] = Some(5)
        lazy val updateDeliveryAddress: DeliveryAddressUpsertion = DeliveryAddressUpsertion()
        lazy val updatePaymentMethodType: Option[PaymentMethodType] = Some(PaymentMethodType.Ekashu)

        lazy val update = CartUpdate(
          email = updateEmail,
          phoneNumber = updatePhoneNumber,
          prepareBy = updatePrepareBy,
          orderType = updateOrderType,
          deliveryAddress = updateDeliveryAddress,
          tipAmount = updateTipAmount,
          paymentMethodType = updatePaymentMethodType,
          checkoutSuccessReturnUrl = Some("https://order-dev.paytouch.io/eatly/success"),
          checkoutFailureReturnUrl = Some("https://order-dev.paytouch.io/eatly/failure"),
        )

        lazy val maxDistance = londonStore.deliveryMaxDistance.get
      }

      "if cart is order_type=take_out and type doesn't change" should {
        "checkout the cart" in new UpdateAndCheckoutContext {
          val orderType = OrderType.TakeOut
          override lazy val updateDeliveryAddress = emptyDeliveryAddress

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertUpdate(cart.id, update)
            assertResponseById(cart.id, entity)
          }
        }
      }

      "if cart is order_type=delivery and type doesn't change" should {
        "if address is within max distance" should {
          "checkout the cart" in new UpdateAndCheckoutContext {
            val orderType = OrderType.Delivery
            override lazy val updateDeliveryAddress = nonEmptyDeliveryAddress

            val cartAddress = nonEmptyAddress.toAddress.get
            GMapsStubData.recordDistance(cartAddress, maxDistance - 1)

            Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }

        "if address is too far away" should {
          "checkout the cart" in new UpdateAndCheckoutContext {
            val orderType = OrderType.Delivery
            override lazy val updateDeliveryAddress = nonEmptyDeliveryAddress

            val cartAddress = nonEmptyAddress.toAddress.get
            GMapsStubData.recordDistance(cartAddress, maxDistance + 1)

            Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("AddressTooFarForDelivery")
            }
          }
        }

        "if update address is empty but cart has address" should {
          "checkout the cart" in new UpdateAndCheckoutContext {
            val orderType = OrderType.Delivery
            override val line1 = Some(genString.instance)
            override val city = Some(genString.instance)
            override val postalCode = Some(genPostalCode.instance)

            override lazy val updateDeliveryAddress = emptyDeliveryAddress

            Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }

        "if order type changes from delivery to take away" should {
          "checkout the cart" in new UpdateAndCheckoutContext {
            val orderType = OrderType.Delivery
            override lazy val updateOrderType = Some(OrderType.TakeOut)

            Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }

        "if order type changes from take away to delivery" should {
          "checkout the cart" in new UpdateAndCheckoutContext {
            val orderType = OrderType.TakeOut
            override lazy val updateOrderType = Some(OrderType.Delivery)
            override lazy val updateDeliveryAddress = nonEmptyDeliveryAddress

            val cartAddress = nonEmptyAddress.toAddress.get
            GMapsStubData.recordDistance(cartAddress, maxDistance - 1)

            Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }
      }

      "if update has negative tip" should {
        "reject the request" in new UpdateAndCheckoutContext {
          val orderType = OrderType.TakeOut
          override lazy val updateTipAmount = Some(-20)

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("NegativeTip")
          }
        }
      }

      "if update has no tip" should {
        "checkout the cart" in new UpdateAndCheckoutContext {
          val orderType = OrderType.TakeOut
          override lazy val updateTipAmount = None

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertUpdate(cart.id, update)
            assertResponseById(cart.id, entity)
          }
        }
      }

      "if paymentMethodType is not supported (different payment processor)" in {
        "reject the request" in new UpdateAndCheckoutContext {
          val orderType = OrderType.TakeOut
          override lazy val updatePaymentMethodType = Some(PaymentMethodType.Worldpay)

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnsupportedPaymentMethod")
          }
        }
      }

      "if paymentMethodType is not supported (cash disabled)" in {
        "reject the request" in new UpdateAndCheckoutContext {
          val orderType = OrderType.TakeOut

          override lazy val londonStore = Factory
            .store(
              merchant,
              locationId = londonId,
              catalogId = londonCatalogId,
              heroImageUrls = londonHeroImageUrl,
              logoImageUrls = londonLogoImageUrl,
              deliveryMinAmount = londonDeliveryMinAmount,
              deliveryMaxAmount = londonDeliveryMaxAmount,
              deliveryFeeAmount = londonDeliveryFeeAmount,
              paymentMethods = Some(
                Seq(
                  PaymentMethod(
                    `type` = PaymentMethodType.Cash,
                    active = false,
                  ),
                  PaymentMethod(
                    `type` = PaymentMethodType.Ekashu,
                    active = true,
                  ),
                ),
              ),
            )
            .create
          override lazy val updatePaymentMethodType = Some(PaymentMethodType.Cash)

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnsupportedPaymentMethod")
          }
        }
      }
    }

    "checkout only" in {
      abstract class CheckoutOnlyContext extends CartsCheckoutFSpecContext {
        val paymentMethodType: Option[PaymentMethodType] = Some(PaymentMethodType.Ekashu)

        lazy val update = CartUpdate(
          email = None,
          phoneNumber = None,
          prepareBy = None,
          orderType = None,
          tipAmount = None,
          deliveryAddress = emptyDeliveryAddress,
          paymentMethodType = paymentMethodType,
          checkoutSuccessReturnUrl = Some("https://order-dev.paytouch.io/eatly/success"),
          checkoutFailureReturnUrl = Some("https://order-dev.paytouch.io/eatly/failure"),
        )
      }

      "if request is for order_type=take_out" should {
        "if outside of delivery boundaries" should {
          "checkout the cart" in new CheckoutOnlyContext {
            val orderType = OrderType.TakeOut
            override lazy val londonDeliveryMaxAmount = Some(1)

            Post(s"/v1/carts.checkout?cart_id=$id", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertResponseById(id, entity)
            }
          }
        }
      }

      "if request is for order_type=delivery" should {
        "if outside delivery boundaries" should {
          "reject the request" in new CheckoutOnlyContext {
            val orderType = OrderType.Delivery
            override val line1 = Some(genString.instance)
            override val city = Some(genString.instance)
            override val postalCode = Some(genPostalCode.instance)

            // forces via tip an amount higher than limit
            override val initialTipAmount: Option[BigDecimal] = Some(15)
            override lazy val londonDeliveryMaxAmount = Some(10)

            Post(s"/v1/carts.checkout?cart_id=$id", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("CartTotalOutOfBounds")
            }
          }
        }

        "if inside delivery boundaries succeed" should {
          "checkout the cart" in new CheckoutOnlyContext {
            val orderType = OrderType.Delivery
            override val line1 = Some(genString.instance)
            override val city = Some(genString.instance)
            override val postalCode = Some(genPostalCode.instance)

            Post(s"/v1/carts.checkout?cart_id=$id", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertResponseById(id, entity)
            }
          }
        }

        "if cart has no address" should {
          "reject the request" in new CheckoutOnlyContext {
            val orderType = OrderType.Delivery

            Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("AddressRequiredForDelivery")
            }
          }
        }
      }

      "if payment method type is not set" should {
        "reject the request" in new CheckoutOnlyContext {
          override val paymentMethodType: Option[PaymentMethodType] = None
          val orderType = OrderType.TakeOut

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("MissingPaymentMethodType")
          }
        }
      }

      "paymentMethodType = ekashu" should {
        trait EkashuCheckoutContext extends CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override val paymentMethodType = Some(PaymentMethodType.Ekashu)
        }

        "sync the cart to core and mark as new" in new EkashuCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertCartStatus(id, status = CartStatus.New, synced = Some(true))
          }
        }

        "returns ekashu payment processor data" in new EkashuCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            entity.paymentProcessorData must beSome

            val paymentProcessorData = entity.paymentProcessorData.get
            paymentProcessorData.reference ==== Some(id.toString)
            paymentProcessorData.hashCodeValue must beSome
          }
        }
      }

      "paymentMethodType = jetdirect" should {
        trait JetdirectCheckoutContext extends CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override val paymentMethodType = Some(PaymentMethodType.Jetdirect)

          override lazy val merchant = Factory
            .merchant(paymentProcessor = Some(PaymentProcessor.Jetdirect))
            .create

          override lazy val londonStore = Factory
            .store(
              merchant,
              locationId = londonId,
              catalogId = londonCatalogId,
              heroImageUrls = londonHeroImageUrl,
              logoImageUrls = londonLogoImageUrl,
              deliveryMinAmount = londonDeliveryMinAmount,
              deliveryMaxAmount = londonDeliveryMaxAmount,
              deliveryFeeAmount = londonDeliveryFeeAmount,
              paymentMethods = Some(
                Seq(
                  PaymentMethod(
                    `type` = PaymentMethodType.Jetdirect,
                    active = true,
                  ),
                ),
              ),
            )
            .create
        }

        "sync the cart to core and mark as new" in new JetdirectCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertCartStatus(id, status = CartStatus.New, synced = Some(true))
          }
        }

        "returns jetdirect payment processor data" in new JetdirectCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            entity.paymentProcessorData must beSome

            val paymentProcessorData = entity.paymentProcessorData.get
            paymentProcessorData.reference ==== Some(id.toString)
            paymentProcessorData.hashCodeValue must beSome
          }
        }
      }

      "paymentMethodType = worldpay" should {
        trait WorldpayCheckoutContext extends CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override val paymentMethodType = Some(PaymentMethodType.Worldpay)

          override lazy val merchant = Factory
            .merchant(paymentProcessor = Some(PaymentProcessor.Worldpay))
            .create

          override lazy val londonStore = Factory
            .store(
              merchant,
              locationId = londonId,
              catalogId = londonCatalogId,
              heroImageUrls = londonHeroImageUrl,
              logoImageUrls = londonLogoImageUrl,
              deliveryMinAmount = londonDeliveryMinAmount,
              deliveryMaxAmount = londonDeliveryMaxAmount,
              deliveryFeeAmount = londonDeliveryFeeAmount,
              paymentMethods = Some(
                Seq(
                  PaymentMethod(
                    `type` = PaymentMethodType.Worldpay,
                    active = true,
                  ),
                ),
              ),
            )
            .create

          val worldpayPaymentDao = daos.worldpayPaymentDao

          def assertWorldpayPayment(
              transactionSetupId: String,
              status: WorldpayPaymentStatus,
              cartId: UUID,
            ) = {
            val maybePayment = worldpayPaymentDao.findByTransactionSetupId(transactionSetupId).await
            maybePayment must beSome
            val payment = maybePayment.get

            payment.status ==== status
            payment.objectId ==== cartId
            payment.objectType ==== WorldpayPaymentType.Cart
          }
        }

        "sync the cart to core and mark as new" in new WorldpayCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertCartStatus(id, status = CartStatus.New, synced = Some(true))
          }
        }

        "returns worldpay payment processor data" in new WorldpayCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            entity.paymentProcessorData must beSome

            val paymentProcessorData = entity.paymentProcessorData.get
            paymentProcessorData.transactionSetupId must beSome
            val transactionSetupId = paymentProcessorData.transactionSetupId.get

            paymentProcessorData.checkoutUrl ==== Some(
              s"http://checkout.worldpay?TransactionSetupId=$transactionSetupId",
            )

            assertWorldpayPayment(transactionSetupId, status = WorldpayPaymentStatus.Submitted, cartId = cart.id)
          }
        }

        "if the store has a currency other than USD" in {
          "reject the request" in new WorldpayCheckoutContext {
            override lazy val londonStore = Factory
              .store(
                merchant,
                locationId = londonId,
                catalogId = londonCatalogId,
                currency = Some(Currency.getInstance("EUR")),
                paymentMethods = Some(
                  Seq(
                    PaymentMethod(
                      `type` = PaymentMethodType.Worldpay,
                      active = true,
                    ),
                  ),
                ),
              )
              .create

            Post(s"/v1/carts.checkout?cart_id=$id", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("UnsupportedPaymentProcessorCurrency")
            }
          }
        }

        "if there are missing return urls" in {
          "reject the request" in new WorldpayCheckoutContext {
            val badUpdate = update.copy(checkoutSuccessReturnUrl = None)

            Post(s"/v1/carts.checkout?cart_id=$id", badUpdate)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("MissingCheckoutReturnUrl")
            }
          }
        }
      }

      "paymentMethodType = stripe" should {
        trait StripeCheckoutContext extends CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override val paymentMethodType = Some(PaymentMethodType.Stripe)

          override lazy val merchant = Factory
            .merchant(paymentProcessor = Some(PaymentProcessor.Stripe))
            .create

          override lazy val londonStore = Factory
            .store(
              merchant,
              locationId = londonId,
              catalogId = londonCatalogId,
              heroImageUrls = londonHeroImageUrl,
              logoImageUrls = londonLogoImageUrl,
              deliveryMinAmount = londonDeliveryMinAmount,
              deliveryMaxAmount = londonDeliveryMaxAmount,
              deliveryFeeAmount = londonDeliveryFeeAmount,
              paymentMethods = Some(
                Seq(
                  PaymentMethod(
                    `type` = PaymentMethodType.Stripe,
                    active = true,
                  ),
                ),
              ),
            )
            .create
        }

        "sync the cart to core and mark as new" in new StripeCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertCartStatus(id, status = CartStatus.New, synced = Some(true))
          }
        }

        "returns stripe payment processor data" in new StripeCheckoutContext {
          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            entity.paymentProcessorData must beSome

            val paymentProcessorConfig = merchant.paymentProcessorConfig.asInstanceOf[StripePaymentProcessorConfig]
            val paymentProcessorData = entity.paymentProcessorData.get
            paymentProcessorData.stripePublishableKey ==== Some(paymentProcessorConfig.publishableKey)
            paymentProcessorData.stripePaymentIntentSecret must beSome
          }
        }
      }

      "paymentMethodType = cash" should {
        // N.b. Renaming paid to submitted would be more accurate
        "sync the cart to core and mark as paid" in new CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override val paymentMethodType = Some(PaymentMethodType.Cash)
          override lazy val acceptanceStatus = AcceptanceStatus.Pending

          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertCartStatus(id, status = CartStatus.Paid, synced = Some(true))
          }
        }
      }

      "paymentMethodType != cash" should {
        "yield PaymentProcessorData.empty if total amount is zero after some gift cards were applied" in new CheckoutOnlyContext {
          override lazy val merchant = Factory
            .merchant(paymentProcessor = Some(PaymentProcessor.Worldpay))
            .create

          override lazy val londonStore = Factory
            .store(
              merchant,
              locationId = londonId,
              catalogId = londonCatalogId,
              heroImageUrls = londonHeroImageUrl,
              logoImageUrls = londonLogoImageUrl,
              deliveryMinAmount = londonDeliveryMinAmount,
              deliveryMaxAmount = londonDeliveryMaxAmount,
              deliveryFeeAmount = londonDeliveryFeeAmount,
              paymentMethods = Some(
                Seq(
                  PaymentMethod(
                    `type` = PaymentMethodType.Worldpay,
                    active = true,
                  ),
                ),
              ),
            )
            .create

          val orderType = OrderType.TakeOut // double check

          override val paymentMethodType = Some(PaymentMethodType.Worldpay)
          override lazy val acceptanceStatus = AcceptanceStatus.Pending

          override lazy val cart = {
            import io.paytouch._

            Factory
              .cart(
                londonStore,
                orderType = orderType.some,
                line1 = line1,
                city = city,
                postalCode = postalCode,
                tipAmount = 0.somew,
                totalAmount = 0.somew,
                appliedGiftCardPasses = Seq(
                  GiftCardPassApplied(
                    id = GiftCardPass.IdPostgres(UUID.randomUUID).cast,
                    onlineCode = GiftCardPass.OnlineCode.Raw("some code"),
                    balance = 10.USD,
                    addedAt = UtcTime.now,
                  ),
                ).some,
              )
              .create
          }

          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertCartStatus(id, status = CartStatus.Paid, synced = Some(true))

            val entity = responseAs[ApiResponse[Cart]].data
            entity.paymentProcessorData ==== PaymentProcessorData.empty.some
          }
        }
      }

      "if cart is paid" should {
        "reject the request" in new CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override lazy val cart =
            Factory.cart(londonStore, orderId = Some(UUID.randomUUID), orderType = Some(OrderType.TakeOut)).create
          cart.status ==== CartStatus.Paid

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ImmutableCart")
          }
        }
      }

      "if cart is synced to core but unpaid" should {
        "checkout the cart" in new CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          override lazy val cart =
            Factory
              .cart(
                londonStore,
                orderId = Some(UUID.randomUUID),
                status = Some(CartStatus.New),
                orderType = Some(OrderType.TakeOut),
              )
              .create
          cart.status ==== CartStatus.New

          Post(s"/v1/carts.checkout?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertUpdate(cart.id, update)
            assertResponseById(cart.id, entity)
          }
        }
      }

      "if a product is out of stock on core" should {
        "reject the request" in new CheckoutOnlyContext {
          val orderType = OrderType.TakeOut
          val cartItem = Factory.cartItem(cart).create

          val response = CoreEmbeddedErrorResponse(
            errors = Seq(
              CoreEmbeddedError(
                code = "ProductOutOfStock",
                message = "Product is out of stock",
                values = Seq(
                  cartItem.productId,
                ),
              ),
            ),
            objectWithErrors = None,
          )

          PtCoreStubData.stubValidatedSyncErrorResponse(response)

          Post(s"/v1/carts.checkout?cart_id=$id", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ProductOutOfStock")
          }
        }
      }
    }

    "if cart id doesn't exist" in {
      "return 404" in new CartsCheckoutFSpecContext {
        val orderType = OrderType.TakeOut
        val invalidId = UUID.randomUUID

        Post(s"/v1/carts.checkout?cart_id=$invalidId")
          .addHeader(storeAuthorizationHeader) ~> routes ~> check {
          rejection ==== ValidationRejection("cart id does not exist")
        }
      }
    }

    "if request has an invalid token" in {
      "reject the request" in new CartsCheckoutFSpecContext {
        val orderType = OrderType.Delivery

        val randomId = UUID.randomUUID
        Post(s"/v1/carts.checkout?cart_id=$randomId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
