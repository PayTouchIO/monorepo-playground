package io.paytouch.ordering.resources.carts

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.clients.paytouch.core.entities.Product
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.CartStatus
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class CartsAddGiftCardFSpec extends CartItemOpsFSpec {
  abstract class CartAddGiftCardFSpecContext extends CartItemOpsFSpecContext with ProductFixtures {
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordProduct(giftCardProduct)
  }

  "POST /v1/carts.add_gift_card" in {
    "if request has valid token" in {
      "with simple gift_card" in {
        "if gift_card exists and is not part of the cart" should {
          "adds item to the cart" in new CartAddGiftCardFSpecContext {
            override lazy val cartItemCreationQuantity: Int = 1

            Post(s"/v1/carts.add_gift_card", giftCardCreation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)

              val entity = responseAs[ApiResponse[Cart]].data

              assertNewItemAdded(
                entity,
                giftCardCreation.toCartItemCreation,
                giftCardProduct,
              )

              val expected =
                giftCardCreation.giftCardData.amount * cartItemCreationQuantity

              assertCartTotals(
                entity,
                subtotal = expected.some,
                total = expected.some,
              )
            }
          }

          "doesn't sync the cart to core" in new CartAddGiftCardFSpecContext {
            Post(s"/v1/carts.add_gift_card", giftCardCreation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Created)
              assertCartStatus(cart.id, status = CartStatus.New, synced = false.some)
            }
          }
        }

        "if gift card exists and is already part of the cart" should {
          "NOT merge item to the cart" in new CartAddGiftCardFSpecContext {
            override lazy val cartItemCreationQuantity: Int = 1

            val cartItem =
              Factory
                .cartItem(
                  cart,
                  productId = giftCardProduct.id.some,
                  quantity = 1.somew,
                  priceAmount = 10.somew,
                  calculatedPriceAmount = 10.somew,
                )
                .create

            Post(s"/v1/carts.add_gift_card", giftCardCreation)
              .addHeader(storeAuthorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val entity = responseAs[ApiResponse[Cart]].data

              entity.items.size ==== 2

              assertNewItemAdded(
                entity,
                giftCardCreation.toCartItemCreation,
                giftCardProduct,
              )

              val expected =
                (giftCardCreation.giftCardData.amount * cartItemCreationQuantity) +
                  cartItem.priceAmount

              assertCartTotals(
                entity,
                subtotal = expected.some,
                total = expected.some,
              )
            }
          }
        }
      }
    }
  }
}
