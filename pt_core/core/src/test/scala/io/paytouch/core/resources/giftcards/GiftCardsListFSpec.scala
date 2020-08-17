package io.paytouch.core.resources.giftcards

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardsListFSpec extends GiftCardsFSpec {

  abstract class GiftCardsListFSpecContext extends GiftCardResourceFSpecContext

  "GET /v1/gift_cards.list" in {
    "if request has valid token" should {
      "when no parameters" in {
        "return a paginated list of gift cards" in new GiftCardResourceFSpecContext {
          val giftCardProduct = Factory.giftCardProduct(merchant).create
          val giftCard = Factory.giftCard(giftCardProduct).create

          Get("/v1/gift_cards.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val giftCards = responseAs[PaginatedApiResponse[Seq[GiftCard]]].data
            giftCards.map(_.id) ==== Seq(giftCard.id)

            assertResponse(giftCards.find(_.id == giftCard.id).get, giftCard)
          }
        }

        "return a paginated list of gift cards with images associated" in new GiftCardResourceFSpecContext {
          val giftCardProduct = Factory.giftCardProduct(merchant).create
          val giftCard = Factory.giftCard(giftCardProduct).create
          val image = Factory
            .imageUpload(merchant, objectId = Some(giftCard.id), imageUploadType = Some(ImageUploadType.GiftCard))
            .create

          Get("/v1/gift_cards.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val giftCards = responseAs[PaginatedApiResponse[Seq[GiftCard]]].data
            giftCards.map(_.id) ==== Seq(giftCard.id)

            assertResponse(giftCards.find(_.id == giftCard.id).get, giftCard, Seq(image))
          }
        }
      }

      "with filter updated_since" in {
        "return a paginated list of gift cards filtered by updated_since" in new GiftCardResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val giftCardProductA = Factory.giftCardProduct(merchant).create
          val giftCardA = Factory.giftCard(giftCardProductA, overrideNow = Some(now)).create

          val giftCardProductB = Factory.giftCardProduct(merchant).create
          val giftCardB = Factory.giftCard(giftCardProductB, overrideNow = Some(now.minusMonths(10))).create

          Get("/v1/gift_cards.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val giftCards = responseAs[PaginatedApiResponse[Seq[GiftCard]]].data
            giftCards.map(_.id) ==== Seq(giftCardA.id)

            assertResponse(giftCards.find(_.id == giftCardA.id).get, giftCardA)
          }
        }
      }
    }
  }

}
