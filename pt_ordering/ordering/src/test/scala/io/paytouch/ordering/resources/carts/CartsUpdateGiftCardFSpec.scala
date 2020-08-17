package io.paytouch.ordering.resources.carts

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ MockedRestApi, FixtureDaoFactory => Factory }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType

class CartsUpdateGiftCardFSpec extends CartItemOpsFSpec {
  abstract class CartUpdateGiftCardSimpleFSpecContext extends CartItemOpsFSpecContext with ProductFixtures

  "POST /v1/carts.update_gift_card_item" in {
    "if request has valid token" in {
      "changes data" in new CartUpdateGiftCardSimpleFSpecContext {
        lazy val cartItem =
          Factory
            .cartItem(
              cart,
              productId = giftCardProduct.id.some,
              quantity = 1.somew,
              priceAmount = 10.somew,
              calculatedPriceAmount = 10.somew,
              `type` = CartItemType.GiftCard.some,
              giftCardData = GiftCardData(
                recipientEmail = "a@b.com",
                amount = 10,
              ).some,
            )
            .create

        val update: GiftCardCartItemUpdate =
          GiftCardCartItemUpdate(
            giftCardData = GiftCardData(
              recipientEmail = "c@d.com",
              amount = 20,
            ),
          )

        Post(s"/v1/carts.update_gift_card_item?cart_id=${cart.id}&cart_item_id=${cartItem.id}", update)
          .addHeader(storeAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[Cart]].data

          assertItemUpdated(
            entity,
            update.toCartItemUpdate,
            cartItem,
            modifierOptionIds = Seq.empty,
          )

          assertCartTotals(
            entity,
            subtotal = update.giftCardData.amount.some,
            total = update.giftCardData.amount.some,
          )
        }
      }
    }
  }
}
