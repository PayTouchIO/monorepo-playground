package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, ValidationRejection }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, OrderType }
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsCreateFSpec extends CartsFSpec {
  abstract class CartCreateFSpecContext extends CartResourceFSpecContext {
    val id = UUID.randomUUID

    @scala.annotation.nowarn("msg=Auto-application")
    val creation =
      random[CartCreation].copy(storeId = romeStore.id, orderType = OrderType.TakeOut)
  }

  "POST /v1/carts.create?cart_id=<cart-id>" in {
    "if request has valid token" in {

      "if merchant has no payment processor" should {
        "create a cart" in new CartCreateFSpecContext {
          override lazy val merchant = Factory.merchant(paymentProcessor = None).create

          Post(s"/v1/carts.create?cart_id=$id", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.Created)

            val entity = responseAs[ApiResponse[Cart]].data
            assertCreation(id, creation)
            assertResponseById(id, entity)
          }
        }
      }
      "if request is for order_type=take_out" should {
        "create a cart of type take out" in new CartCreateFSpecContext {
          val takeoutCreation = creation.copy(orderType = OrderType.TakeOut)

          Post(s"/v1/carts.create?cart_id=$id", takeoutCreation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.Created)

            val entity = responseAs[ApiResponse[Cart]].data
            assertCreation(id, takeoutCreation)
            assertResponseById(id, entity)
          }
        }
      }

      "if request is for order_type=delivery" should {
        "if request has non empty address" should {
          "create a cart" in new CartCreateFSpecContext {
            val deliveryCreation =
              creation.copy(orderType = OrderType.Delivery, deliveryAddress = nonEmptyDeliveryAddress)

            Post(s"/v1/carts.create?cart_id=$id", deliveryCreation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)

              val entity = responseAs[ApiResponse[Cart]].data
              assertCreation(id, deliveryCreation)
              assertResponseById(id, entity)
            }
          }
        }

        "if request has an empty address" should {
          "reject the request" in new CartCreateFSpecContext {
            val invalidId = UUID.randomUUID

            val invalidCreation = creation.copy(orderType = OrderType.Delivery, deliveryAddress = emptyDeliveryAddress)

            Post(s"/v1/carts.create?cart_id=$id", invalidCreation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("AddressRequiredForDelivery")
            }
          }
        }

        "if request has an empty delivery address" should {
          "reject the request" in new CartCreateFSpecContext {
            val invalidId = UUID.randomUUID
            val invalidCreation = creation.copy(orderType = OrderType.Delivery, deliveryAddress = emptyDeliveryAddress)

            Post(s"/v1/carts.create?cart_id=$id", invalidCreation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCode("AddressRequiredForDelivery")
            }
          }
        }

        "doesn't sync the cart to core" in new CartCreateFSpecContext {
          Post(s"/v1/carts.create?cart_id=$id", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.Created)
            assertCartStatus(id, status = CartStatus.New, synced = Some(false))
          }
        }
      }

      "if cart id is already taken" should {
        "reject the request" in new CartCreateFSpecContext {
          val cart = Factory.cart(londonStore).create

          Post(s"/v1/carts.create?cart_id=${cart.id}", creation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if store id does not exist" should {
        "reject the request" in new CartCreateFSpecContext {
          val invalidCreation = creation.copy(storeId = UUID.randomUUID)

          Post(s"/v1/carts.create?cart_id=$id", invalidCreation)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            rejection ==== ValidationRejection("store id does not exist")
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new CartCreateFSpecContext {
        Post(s"/v1/carts.create?cart_id=$id", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
