package io.paytouch.core.resources.giftcards

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardsUpdateActiveFSpec extends GiftCardsFSpec {
  "POST /v1/gift_cards.update_active" in {
    "if request has valid token" in {
      "if gift cards belong to the merchant" should {
        "disable/enable gift_cards " in new GiftCardResourceFSpecContext {
          val giftCardProduct = Factory.giftCardProduct(merchant).create
          val giftCardToDisableA = Factory.giftCard(giftCardProduct, active = Some(true)).create
          val giftCardToDisableB = Factory.giftCard(giftCardProduct, active = Some(false)).create
          val giftCardToActivateA = Factory.giftCard(giftCardProduct, active = Some(true)).create
          val giftCardToActivateB = Factory.giftCard(giftCardProduct, active = Some(false)).create

          val giftCardActiveUpdateItem = Seq(
            UpdateActiveItem(giftCardToDisableA.id, false),
            UpdateActiveItem(giftCardToDisableB.id, false),
            UpdateActiveItem(giftCardToActivateA.id, true),
            UpdateActiveItem(giftCardToActivateB.id, true),
          )

          Post(s"/v1/gift_cards.update_active", giftCardActiveUpdateItem)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            afterAWhile {
              giftCardDao.findById(giftCardToDisableA.id).await.get.active should beFalse
              giftCardDao.findById(giftCardToDisableB.id).await.get.active should beFalse
              giftCardDao.findById(giftCardToActivateA.id).await.get.active should beTrue
              giftCardDao.findById(giftCardToActivateB.id).await.get.active should beTrue
            }
          }
        }
      }

      "if gift card doesn't belong to the merchant" should {
        "not update the gift card and return 404" in new GiftCardResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCardToDisable = Factory.giftCard(competitorGiftCardProduct, active = Some(true)).create
          val giftCardActiveUpdateItem = Seq(
            UpdateActiveItem(competitorGiftCardToDisable.id, false),
          )

          Post(s"/v1/gift_cards.update_active", giftCardActiveUpdateItem)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            giftCardDao.findById(competitorGiftCardToDisable.id).await.get.active should beTrue
          }
        }
      }
    }
  }
}
