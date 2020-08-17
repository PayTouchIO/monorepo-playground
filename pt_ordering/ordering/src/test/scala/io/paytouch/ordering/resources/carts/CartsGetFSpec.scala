package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.utils.{ UtcTime, FixtureDaoFactory => Factory }

final class CartsGetFSpec extends CartsFSpec {
  "GET /v1/carts.get?cart_id=<cart-id>" in {
    "if request has valid token" in {
      "for an ekashu merchant" in {
        "return a cart" in new CartResourceFSpecContext {
          val cart = Factory.cart(romeStore).create
          val cartTaxRate = Factory.cartTaxRate(cart).create

          val cartItem1 = Factory.cartItem(cart, overrideNow = Some(UtcTime.now.minusMinutes(2))).create
          val cartItem2 = Factory.cartItem(cart, overrideNow = Some(UtcTime.now.minusMinutes(3))).create
          val cartItem3 = Factory.cartItem(cart, overrideNow = Some(UtcTime.now.minusMinutes(1))).create
          val cartItemModifierOption = Factory.cartItemModifierOption(cartItem1).create
          val cartItemTaxRate = Factory.cartItemTaxRate(cartItem1, value = Some(8.375)).create
          cartItemTaxRate.`value` ==== 8.375

          val cartItemVariantOption = Factory.cartItemVariantOption(cartItem1).create
          Get(s"/v1/carts.get?cart_id=${cart.id}").addHeader(storeAuthorizationHeader) ~> routes ~> check {
            val entity = responseAs[ApiResponse[Cart]].data
            assertResponse(
              entity,
              cart,
              cartTaxRates = Seq(cartTaxRate),
              cartItems = Seq(cartItem2, cartItem1, cartItem3),
              cartItemModifierOptions = Map(cartItem1 -> Seq(cartItemModifierOption)),
              cartItemTaxRates = Map(cartItem1 -> Seq(cartItemTaxRate)),
              cartItemVariantOptions = Map(cartItem1 -> Seq(cartItemVariantOption)),
            )
          }
        }
      }

      "for a jetdirect merchant" in {
        "return a cart" in new CartResourceFSpecContext {
          override lazy val merchant = Factory
            .merchant(paymentProcessor = Some(PaymentProcessor.Jetdirect))
            .create
          val cart = Factory.cart(romeStore).create
          val cartTaxRate = Factory.cartTaxRate(cart).create

          val cartItem1 = Factory.cartItem(cart, overrideNow = Some(UtcTime.now.minusMinutes(2))).create
          val cartItem2 = Factory.cartItem(cart, overrideNow = Some(UtcTime.now.minusMinutes(3))).create
          val cartItem3 = Factory.cartItem(cart, overrideNow = Some(UtcTime.now.minusMinutes(1))).create
          val cartItemModifierOption = Factory.cartItemModifierOption(cartItem1).create
          val cartItemTaxRate = Factory.cartItemTaxRate(cartItem1).create
          val cartItemVariantOption = Factory.cartItemVariantOption(cartItem1).create
          Get(s"/v1/carts.get?cart_id=${cart.id}").addHeader(storeAuthorizationHeader) ~> routes ~> check {
            val entity = responseAs[ApiResponse[Cart]].data
            assertResponse(
              entity,
              cart,
              cartTaxRates = Seq(cartTaxRate),
              cartItems = Seq(cartItem2, cartItem1, cartItem3),
              cartItemModifierOptions = Map(cartItem1 -> Seq(cartItemModifierOption)),
              cartItemTaxRates = Map(cartItem1 -> Seq(cartItemTaxRate)),
              cartItemVariantOptions = Map(cartItem1 -> Seq(cartItemVariantOption)),
            )
          }
        }
      }
    }

    "if request has an invalid token" in {
      "reject the request" in new CartResourceFSpecContext {
        val cartId = UUID.randomUUID
        Get(s"/v1/carts.get?cart_id=$cartId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
