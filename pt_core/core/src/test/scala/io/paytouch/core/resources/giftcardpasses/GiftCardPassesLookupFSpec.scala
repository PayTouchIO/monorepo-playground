package io.paytouch.core.resources.giftcardpasses

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardPassesLookupFSpec extends GiftCardPassesFSpec {
  abstract class GiftCardPassesLookupFSpecContext extends GiftCardPassResourceFSpecContext

  "GET /v1/gift_card_passes.lookup?lookup_id=$" in {
    "if request has valid token" should {
      "with no parameters" should {
        "return a gift card pass" in new GiftCardPassesLookupFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem, lookupId = Some("giftA123")).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          val anotherGiftCardPass =
            Factory.giftCardPass(giftCard, orderItem, lookupId = Some("yoyoyo")).create

          Get(s"/v1/gift_card_passes.lookup?lookup_id=${giftCardPass.lookupId}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass)
          }
        }
      }

      "with expand[]=transactions" should {
        "return a gift card passes with transactions expanded" in new GiftCardPassesLookupFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem, lookupId = Some("giftA123")).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          val anotherGiftCardPass =
            Factory.giftCardPass(giftCard, orderItem, lookupId = Some("yoyoyo")).create

          Get(s"/v1/gift_card_passes.lookup?lookup_id=${giftCardPass.lookupId}&expand[]=transactions")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass, Seq(transaction1, transaction2))
          }
        }
      }

      "if the gift card pass lookup id belongs to another merchant" should {
        "return 404" in new GiftCardPassesLookupFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create
          val competitorCustomer = Factory.globalCustomer(Some(competitor)).create
          val competitorOrder = Factory.order(competitor).create
          val competitorOrderItem = Factory.orderItem(competitorOrder).create

          val competitorGiftCardPass =
            Factory
              .giftCardPass(competitorGiftCard, competitorOrderItem, lookupId = Some("my-lookup-id"))
              .create

          Get(s"/v1/gift_card_passes.lookup?lookup_id=${competitorGiftCardPass.lookupId}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the gift card pass lookup id does not exist" should {
        "return 404" in new GiftCardPassesLookupFSpecContext {
          Get(s"/v1/gift_card_passes.lookup?lookup_id=whatever")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  "GET /v1/gift_card_passes.lookup?online_code=$" in {
    "if request has valid token" should {
      "with no parameters" should {
        "return a gift card pass" in new GiftCardPassesLookupFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          val anotherGiftCardPass =
            Factory.giftCardPass(giftCard, orderItem).create

          Get(s"/v1/gift_card_passes.lookup?online_code=${giftCardPass.onlineCode.value}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass)
          }
        }

        "return a gift card pass even if it contains hyphens and is mixed case" in new GiftCardPassesLookupFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          val anotherGiftCardPass =
            Factory.giftCardPass(giftCard, orderItem).create

          val onlineCode: String =
            giftCardPass
              .onlineCode
              .value
              .toLowerCase
              .hyphenatedAfterEvery(position = 4)

          Get(s"/v1/gift_card_passes.lookup?online_code=$onlineCode")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass)
          }
        }
      }

      "with expand[]=transactions" should {
        "return a gift card passes with transactions expanded" in new GiftCardPassesLookupFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          val anotherGiftCardPass =
            Factory.giftCardPass(giftCard, orderItem, lookupId = Some("yoyoyo")).create

          Get(s"/v1/gift_card_passes.lookup?online_code=${giftCardPass.onlineCode.value}&expand[]=transactions")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass, Seq(transaction1, transaction2))
          }
        }
      }

      "if the gift card pass lookup id belongs to another merchant" should {
        "return 404" in new GiftCardPassesLookupFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create
          val competitorCustomer = Factory.globalCustomer(Some(competitor)).create
          val competitorOrder = Factory.order(competitor).create
          val competitorOrderItem = Factory.orderItem(competitorOrder).create

          val competitorGiftCardPass =
            Factory
              .giftCardPass(competitorGiftCard, competitorOrderItem)
              .create

          Get(s"/v1/gift_card_passes.lookup?online_code=${competitorGiftCardPass.onlineCode.value}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the gift card pass lookup id does not exist" should {
        "return 404" in new GiftCardPassesLookupFSpecContext {
          Get(s"/v1/gift_card_passes.lookup?online_code=whatever")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
