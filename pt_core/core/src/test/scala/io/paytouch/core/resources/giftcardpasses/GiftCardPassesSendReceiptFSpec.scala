package io.paytouch.core.resources.giftcardpasses

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.SendReceiptData
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardPassesSendReceiptFSpec extends GiftCardPassesFSpec {

  abstract class GiftCardPassesSendReceiptFSpecContext extends GiftCardPassResourceFSpecContext {

    def assertRecipientEmailUpdated(giftCardPassId: UUID, expectedEmail: String) = {
      val maybeGiftCardPass = giftCardPassDao.findById(giftCardPassId).await
      maybeGiftCardPass must beSome
      val giftCardPass = maybeGiftCardPass.get
      giftCardPass.recipientEmail ==== Some(expectedEmail)
    }
  }

  trait Fixtures { self: GiftCardPassesSendReceiptFSpecContext =>
    val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
  }

  "POST /v1/gift_card_passes.send_receipt?order_item_id=$" in {
    "if request has valid token" in {

      "if the order belongs to the current merchant" should {
        "if email is invalid" should {
          "return 400" in new GiftCardPassesSendReceiptFSpecContext with Fixtures {
            val sendReceiptData = SendReceiptData(recipientEmail = "wrongemail")
            Post(s"/v1/gift_card_passes.send_receipt?order_item_id=${orderItem.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertErrorCodesAtLeastOnce("InvalidEmail")
            }
          }
        }

        "update the gift card pass with the given email" in new GiftCardPassesSendReceiptFSpecContext with Fixtures {
          val sendReceiptData = SendReceiptData(randomEmail)

          Post(s"/v1/gift_card_passes.send_receipt?order_item_id=${orderItem.id}", sendReceiptData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertRecipientEmailUpdated(giftCardPass.id, sendReceiptData.recipientEmail.toString)
          }
        }
      }

      "if the gift card pass doesn't exist" should {
        "reject the request" in new GiftCardPassesSendReceiptFSpecContext {
          val sendReceiptData = random[SendReceiptData]
          Post(s"/v1/gift_card_passes.send_receipt?order_item_id=${UUID.randomUUID}", sendReceiptData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("InvalidGiftCardPassOrderItemAssociation")
          }
        }
      }

      "if the order doesn't belong to the current merchant" should {
        "reject the request" in new GiftCardPassesSendReceiptFSpecContext {
          val sendReceiptData = random[SendReceiptData]
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create
          val competitorOrder = Factory.order(competitor).create
          val competitorOrderItem =
            Factory.orderItem(competitorOrder, product = Some(competitorGiftCardProduct)).create

          Post(s"/v1/gift_card_passes.send_receipt?order_item_id=${competitorOrderItem.id}", sendReceiptData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("InvalidGiftCardPassOrderItemAssociation")
          }
        }
      }
    }
  }
}
