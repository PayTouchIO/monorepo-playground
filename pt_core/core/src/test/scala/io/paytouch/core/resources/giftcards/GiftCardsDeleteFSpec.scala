package io.paytouch.core.resources.giftcards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardsDeleteFSpec extends GiftCardsFSpec {
  abstract class GiftCardDeleteResourceFSpecContext extends GiftCardResourceFSpecContext {
    def assertGiftCardDeleted(giftCardId: UUID) =
      giftCardDao.findById(giftCardId).await should beNone

    def assertGiftCardNotDeleted(giftCardId: UUID) =
      giftCardDao.findById(giftCardId).await should beSome

    def assertGiftCardProductMarkDeleted(productId: UUID) = {
      val product = articleDao.findDeletedById(productId).await
      product should beSome
      product.flatMap(_.deletedAt) should beSome
    }

    def assertGiftCardProductMarkNonDeleted(productId: UUID) = {
      val product = articleDao.findDeletedById(productId).await
      product should beSome
      product.flatMap(_.deletedAt) should beNone
    }
  }

  "POST /v1/gift_cards.delete" in {
    "if request has valid token" in {
      "if giftCard doesn't exist" should {
        "do nothing and return 204" in new GiftCardDeleteResourceFSpecContext {
          val nonExistingGiftCardId = UUID.randomUUID

          Post(s"/v1/gift_cards.delete", Ids(ids = Seq(nonExistingGiftCardId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertGiftCardDeleted(nonExistingGiftCardId)
          }
        }
      }

      "if gift card belongs to the merchant" should {
        "delete the gift card and mark the product as deleted and return 204" in new GiftCardDeleteResourceFSpecContext {
          val giftCardProduct = Factory.giftCardProduct(merchant).create
          val giftCard = Factory.giftCard(giftCardProduct).create

          Post(s"/v1/gift_cards.delete", Ids(ids = Seq(giftCard.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertGiftCardDeleted(giftCard.id)
            assertGiftCardProductMarkDeleted(giftCardProduct.id)
          }
        }
      }

      "if gift card belongs to a different merchant" should {
        "do not delete the gift card and its product and return 204" in new GiftCardDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create

          Post(s"/v1/gift_cards.delete", Ids(ids = Seq(competitorGiftCard.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertGiftCardNotDeleted(competitorGiftCard.id)
            assertGiftCardProductMarkNonDeleted(competitorGiftCardProduct.id)
          }
        }
      }
    }
  }
}
