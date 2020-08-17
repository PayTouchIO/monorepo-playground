package io.paytouch.core.resources.giftcards

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardsGetFSpec extends GiftCardsFSpec {

  abstract class GiftCardsGetFSpecContext extends GiftCardResourceFSpecContext

  "GET /v1/gift_cards.get?gift_card_id=$" in {
    "if request has valid token" should {
      "return a gift card" in new GiftCardsGetFSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct).create

        Get(s"/v1/gift_cards.get?gift_card_id=${giftCard.id}").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[GiftCard]].data
          assertResponse(entity, giftCard)
        }
      }

      "return a gift card with images associated" in new GiftCardsGetFSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct).create
        val image = Factory
          .imageUpload(merchant, objectId = Some(giftCard.id), imageUploadType = Some(ImageUploadType.GiftCard))
          .create

        Get(s"/v1/gift_cards.get?gift_card_id=${giftCard.id}").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[GiftCard]].data
          assertResponse(entity, giftCard, Seq(image))
        }
      }

      "if the gift card does not belong to the merchant" should {
        "return 404" in new GiftCardsGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create

          Get(s"/v1/gift_cards.get?gift_card_id=${competitorGiftCard.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }

}
