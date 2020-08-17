package io.paytouch.core.resources.giftcards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardsUpdateFSpec extends GiftCardsFSpec {

  abstract class GiftCardsUpdateFSpecContext extends GiftCardResourceFSpecContext

  "POST /v1/gift_cards.update?gift_card_id=$" in {
    "if request has valid token" in {

      "update gift card and return 200" in new GiftCardsUpdateFSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct).create

        val image = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.GiftCard)).create

        val update = random[GiftCardUpdate].copy(upc = randomUpc, imageUploadIds = Some(Seq(image.id)))

        Post(s"/v1/gift_cards.update?gift_card_id=${giftCard.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val giftCard = responseAs[ApiResponse[GiftCard]].data
          assertUpdate(giftCard.id, update)
        }
      }

      "if upc is already in use" in new GiftCardsUpdateFSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct).create

        val product = Factory.simpleProduct(merchant, upc = Some(randomUpc)).create

        val update = random[GiftCardUpdate].copy(upc = product.upc)

        Post(s"/v1/gift_cards.update?gift_card_id=${giftCard.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new GiftCardsUpdateFSpecContext {
        val newGiftCardId = UUID.randomUUID
        val update = random[GiftCardUpdate]
        Post(s"/v1/gift_cards.update?gift_card_id=$newGiftCardId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
