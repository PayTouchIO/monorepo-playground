package io.paytouch.ordering.resources.carts

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._

import io.paytouch.ordering.entities._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

class CartsUpdateItemRejectionsFSpec extends CartItemOpsFSpec {
  abstract class CartUpdateProductFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    PtCoreStubData.recordProduct(product)(MockedRestApi.ptCoreClient.generateAuthHeaderForCore)

    @scala.annotation.nowarn("msg=Auto-application")
    val baseUpdate =
      random[CartItemUpdate]
        .copy(
          productId = None,
          quantity = Some(2),
          modifierOptions = None,
        )

    val cartItem =
      Factory
        .cartItem(
          cart,
          productId = product.id.some,
        )
        .create
  }

  "POST /v1/carts.update_product_item" in {
    "if request has valid token" in {
      "if cart id does not exist" should {
        "reject the request" in new CartUpdateProductFSpecContext {
          val invalidCartId = UUID.randomUUID

          Post(s"/v1/carts.update_product_item?cart_id=$invalidCartId&cart_item_id=${cartItem.id}", baseUpdate)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            rejection ==== ValidationRejection("cart id does not exist")
          }
        }
      }

      "if cart item id does not exist" should {
        "reject the request" in new CartUpdateProductFSpecContext {
          val invalidCartItemId = UUID.randomUUID

          Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=$invalidCartItemId", baseUpdate)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("InvalidCartItemIds")
          }
        }
      }

      "if modifier option id does not exist in core" should {
        "reject the request" in new CartUpdateProductFSpecContext {
          val modifierOptionCreation = random[CartItemModifierOptionCreation]
          val invalidUpdate = baseUpdate.copy(modifierOptions = Some(Seq(modifierOptionCreation)))

          Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", invalidUpdate)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("InvalidModifierOptionIds")
          }
        }
      }

      "if product id does not exist in core" should {
        "reject the request" in new CartUpdateProductFSpecContext {
          val invalidUpdate = baseUpdate.copy(productId = Some(UUID.randomUUID))

          Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", invalidUpdate)
            .addHeader(storeAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.InternalServerError)
            assertErrorCode("InternalServerError")
          }
        }
      }
    }

    "if request has an invalid token" in {
      "reject the request" in new CartUpdateProductFSpecContext {
        Post(s"/v1/carts.update_product_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", baseUpdate)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
