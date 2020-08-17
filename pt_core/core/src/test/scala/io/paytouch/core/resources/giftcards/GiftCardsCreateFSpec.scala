package io.paytouch.core.resources.giftcards

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.ServiceConfigurations
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

class GiftCardsCreateFSpec extends GiftCardsFSpec {
  abstract class GiftCardsCreateFSpecContext extends GiftCardResourceFSpecContext

  "POST /v1/gift_cards.create?gift_card_id=$" in {
    "if request has valid token" in {

      "create gift card and return 201" in new GiftCardsCreateFSpecContext {
        val newGiftCardId = UUID.randomUUID

        val validUpc = randomUpc
        val image = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.GiftCard)).create

        val creation = random[GiftCardCreation].copy(upc = validUpc, imageUploadIds = Some(Seq(image.id)))

        lazy val walletClient = MockedRestApi.walletClient

        walletClient.createTemplateWithProjectIdAndExternalId(any, any, any) returns Future.successful(
          Right(TemplateUpserted("newTemplateId1")),
        ) thenReturns Future.successful(Right(TemplateUpserted("newTemplateId2")))

        Post(s"/v1/gift_cards.create?gift_card_id=$newGiftCardId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val giftCard = responseAs[ApiResponse[GiftCard]].data
          assertCreation(giftCard.id, creation)
        }
      }

      "if upc is already in use" in new GiftCardsCreateFSpecContext {
        val newGiftCardId = UUID.randomUUID

        val product = Factory.simpleProduct(merchant, upc = Some(randomUpc)).create

        val creation = random[GiftCardCreation].copy(upc = product.upc)

        Post(s"/v1/gift_cards.create?gift_card_id=$newGiftCardId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }

      "a gift card already exists for the merchant" in new GiftCardsCreateFSpecContext {
        val existingGiftCardProduct = Factory.giftCardProduct(merchant).create
        val existingGiftCard = Factory.giftCard(existingGiftCardProduct).create

        val newGiftCardId = UUID.randomUUID

        val creation = random[GiftCardCreation].copy(upc = randomUpc)

        Post(s"/v1/gift_cards.create?gift_card_id=$newGiftCardId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new GiftCardsCreateFSpecContext {
        val newGiftCardId = UUID.randomUUID
        val creation = random[GiftCardCreation]
        Post(s"/v1/gift_cards.create?gift_card_id=$newGiftCardId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
