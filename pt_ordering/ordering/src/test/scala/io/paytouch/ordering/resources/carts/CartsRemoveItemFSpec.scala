package io.paytouch.ordering.resources.carts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._

import cats.implicits._

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.MonetaryAmount._
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory, _ }

class CartsRemoveItemFSpec extends CartsFSpec with CommonArbitraries {
  abstract class CartsRemoveItemFSpecContext extends CartResourceFSpecContext {
    val cart = Factory
      .cart(
        romeStore,
        // This is only here to make sure that item removal is not affected by applied gift cards
        appliedGiftCardPasses = Seq(
          GiftCardPassApplied(
            id = io.paytouch.GiftCardPass.IdPostgres(UUID.randomUUID).cast,
            onlineCode = io.paytouch.GiftCardPass.OnlineCode.Raw("whatever"),
            balance = 10.USD,
            addedAt = UtcTime.now,
          ),
        ).some,
      )
      .create
    val cartItem = Factory.cartItem(cart).create
    implicit val storeContext = StoreContext.fromRecord(romeStore)
  }

  "POST /v1/carts.remove_item?cart_id=$&cart_item_id=$" in {
    "if request has valid token" in {

      "if cart item id exists" should {
        "remove cart item and all its relations" in new CartsRemoveItemFSpecContext {
          val cartItemModifierOption = Factory.cartItemModifierOption(cartItem).create
          val cartItemTaxRate = Factory.cartItemTaxRate(cartItem).create
          val cartItemVariantOption = Factory.cartItemVariantOption(cartItem).create

          Post(s"/v1/carts.remove_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}")
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val entity = responseAs[ApiResponse[Cart]].data
            cartItemDao.findById(cartItem.id).await must beNone
            cartItemModifierOptionDao.findById(cartItemModifierOption.id).await must beNone
            cartItemTaxRateDao.findById(cartItemTaxRate.id).await must beNone
            cartItemVariantOptionDao.findById(cartItemVariantOption.id).await must beNone
          }
        }
      }

      "if cart doesn't exists" should {
        "reject the request" in new CartsRemoveItemFSpecContext {

          Post(s"/v1/carts.remove_item?cart_id=${UUID.randomUUID}&cart_item_id=${UUID.randomUUID}")
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            rejection ==== ValidationRejection("cart id does not exist")
          }
        }
      }

      "if cart item doesn't exists" should {
        "do nothing" in new CartsRemoveItemFSpecContext {

          Post(s"/v1/carts.remove_item?cart_id=${cart.id}&cart_item_id=${UUID.randomUUID}")
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new CartsRemoveItemFSpecContext {
        Post(s"/v1/carts.remove_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
