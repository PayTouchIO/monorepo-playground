package io.paytouch.core.resources.giftcardpasses

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardPassesGetFSpec extends GiftCardPassesFSpec {

  abstract class GiftCardPassesGetFSpecContext extends GiftCardPassResourceFSpecContext

  "GET /v1/gift_card_passes.get?gift_card_pass_id=$" in {
    "if request has valid token" should {
      "with no parameters" should {
        "return a gift card pass" in new GiftCardPassesGetFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          Get(s"/v1/gift_card_passes.get?gift_card_pass_id=${giftCardPass.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass)
          }
        }
      }

      "with expand[]=transactions" should {
        "return a gift card passes with transactions expanded" in new GiftCardPassesGetFSpecContext {
          val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
          val transaction1 = Factory.giftCardPassTransaction(giftCardPass).create
          val transaction2 = Factory.giftCardPassTransaction(giftCardPass).create

          Get(s"/v1/gift_card_passes.get?gift_card_pass_id=${giftCardPass.id}&expand[]=transactions")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[GiftCardPass]].data
            assertResponse(entity, giftCardPass, Seq(transaction1, transaction2))
          }
        }
      }

      "if the gift card pass does not belong to the merchant" should {
        "return 404" in new GiftCardPassesGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create
          val competitorOrder = Factory.order(competitor).create
          val competitorOrderItem = Factory.orderItem(competitorOrder).create

          val competitorGiftCardPass =
            Factory.giftCardPass(competitorGiftCard, competitorOrderItem).create

          Get(s"/v1/gift_card_passes.get?gift_card_pass_id=${competitorGiftCardPass.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }

}
