package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, ValidationRejection }

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType, PaymentMethodType }
import io.paytouch.ordering.stubs.GMapsStubData
import io.paytouch.ordering.utils.{ CommonArbitraries, FixtureDaoFactory => Factory }

class CartsUpdateFSpec extends CartsFSpec with CommonArbitraries {
  abstract class CartUpdateFSpecContext extends CartResourceFSpecContext {
    val cart = Factory.cart(romeStore, orderType = Some(OrderType.TakeOut)).create

    lazy val updateOrderType: Option[OrderType] = None
    lazy val updateTipAmount: BigDecimal = 5
    lazy val updateDeliveryAddress: DeliveryAddressUpsertion = DeliveryAddressUpsertion()
    lazy val updatePaymentMethodType: Option[PaymentMethodType] = None

    @scala.annotation.nowarn("msg=Auto-application")
    lazy val update =
      random[CartUpdate].copy(
        orderType = updateOrderType,
        deliveryAddress = updateDeliveryAddress,
        tipAmount = Some(updateTipAmount),
        paymentMethodType = updatePaymentMethodType,
      )

    implicit val uah: Authorization = userAuthorizationHeader
    lazy val maxDistance = romeStore.deliveryMaxDistance.get
  }

  "POST /v1/carts.update?cart_id=<cart-id>" in {
    "if request has valid token" in {

      "if request is for take-out" should {
        "update a cart" in new CartUpdateFSpecContext {
          override val cart = Factory.cart(romeStore, orderType = Some(OrderType.TakeOut)).create

          Post(s"/v1/carts.update?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertUpdate(cart.id, update)
            assertResponseById(cart.id, entity)
          }

        }
      }

      "if request is for delivery" should {
        "if address is non-empty and within max distance" should {
          "update a cart" in new CartUpdateFSpecContext {
            override val cart = Factory.cart(romeStore, orderType = Some(OrderType.Delivery)).create
            override lazy val updateDeliveryAddress = nonEmptyDeliveryAddress

            val cartAddress = nonEmptyAddress.toAddress.get
            GMapsStubData.recordDistance(cartAddress, maxDistance - 1)

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }

        "if address is non-empty and too far away" should {
          "update a cart" in new CartUpdateFSpecContext {
            override val cart = Factory.cart(romeStore, orderType = Some(OrderType.Delivery)).create
            override lazy val updateDeliveryAddress = nonEmptyDeliveryAddress

            val cartAddress = nonEmptyAddress.toAddress.get
            GMapsStubData.recordDistance(cartAddress, maxDistance + 1)

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("AddressTooFarForDelivery")
            }
          }
        }

        "if address is empty" should {
          "reject the request" in new CartUpdateFSpecContext {
            override val cart = Factory.cart(romeStore, orderType = Some(OrderType.Delivery)).create
            override lazy val updateDeliveryAddress = emptyDeliveryAddress

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("AddressRequiredForDelivery")
            }
          }
        }

        "if delivery address is empty" should {
          "reject the request" in new CartUpdateFSpecContext {
            override val cart = Factory.cart(romeStore, orderType = Some(OrderType.Delivery)).create

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("AddressRequiredForDelivery")
            }
          }
        }

        "if address is empty but the address in the db is not" should {
          "update a cart" in new CartUpdateFSpecContext {
            override val cart =
              Factory
                .cart(
                  romeStore,
                  orderType = Some(OrderType.Delivery),
                  line1 = Some(genString.instance),
                  city = Some(genString.instance),
                  postalCode = Some(genPostalCode.instance),
                )
                .create
            override lazy val updateDeliveryAddress = emptyDeliveryAddress

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }

        "if order type changes from delivery to take away" should {
          "update the cart" in new CartUpdateFSpecContext {
            override val cart = Factory.cart(romeStore, orderType = Some(OrderType.Delivery)).create
            override lazy val updateOrderType = Some(OrderType.TakeOut)

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }

        "if order type changes from take away to delivery" should {
          "update the cart" in new CartUpdateFSpecContext {
            override val cart = Factory.cart(romeStore, orderType = Some(OrderType.TakeOut)).create
            override lazy val updateOrderType = Some(OrderType.Delivery)
            override lazy val updateDeliveryAddress = nonEmptyDeliveryAddress

            val cartAddress = nonEmptyAddress.toAddress.get
            GMapsStubData.recordDistance(cartAddress, maxDistance - 1)

            Post(s"/v1/carts.update?cart_id=${cart.id}", update)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Cart]].data
              assertUpdate(cart.id, update)
              assertResponseById(cart.id, entity)
            }
          }
        }
      }

      "if paymentMethodType is not supported (different payment processor)" in {
        "reject the request" in new CartUpdateFSpecContext {
          override lazy val updatePaymentMethodType = Some(PaymentMethodType.Worldpay)

          Post(s"/v1/carts.update?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnsupportedPaymentMethod")
          }
        }
      }

      "if paymentMethodType is not supported (cash disabled)" in {
        "reject the request" in new CartUpdateFSpecContext {
          override lazy val romeStore = Factory
            .store(
              merchant,
              locationId = romeId,
              catalogId = romeCatalogId,
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

          Post(s"/v1/carts.update?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UnsupportedPaymentMethod")
          }
        }
      }

      "if cart id doesn't exist " in {
        "return 404" in new CartUpdateFSpecContext {
          val invalidId = UUID.randomUUID

          Post(s"/v1/carts.update?cart_id=$invalidId", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            rejection ==== ValidationRejection("cart id does not exist")
          }
        }
      }

      "if cart is paid" should {
        "reject the request" in new CartUpdateFSpecContext {
          override val cart =
            Factory.cart(romeStore, orderId = Some(UUID.randomUUID), orderType = Some(OrderType.TakeOut)).create
          cart.status ==== CartStatus.Paid

          Post(s"/v1/carts.update?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("ImmutableCart")
          }
        }
      }

      "if cart is synced to core but unpaid" should {
        "update a cart" in new CartUpdateFSpecContext {
          override val cart =
            Factory
              .cart(
                romeStore,
                orderId = Some(UUID.randomUUID),
                status = Some(CartStatus.New),
                orderType = Some(OrderType.TakeOut),
              )
              .create
          cart.status ==== CartStatus.New

          Post(s"/v1/carts.update?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Cart]].data
            assertUpdate(cart.id, update)
            assertResponseById(cart.id, entity)
          }
        }
      }

      "if cart has negative tip" should {
        "reject the request" in new CartUpdateFSpecContext {
          override lazy val updateTipAmount = -20

          Post(s"/v1/carts.update?cart_id=${cart.id}", update)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("NegativeTip")
          }

        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new CartUpdateFSpecContext {
        Post(s"/v1/carts.update?cart_id=${cart.id}", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
