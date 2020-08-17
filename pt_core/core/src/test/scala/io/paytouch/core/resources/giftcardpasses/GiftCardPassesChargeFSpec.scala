package io.paytouch.core.resources.giftcardpasses

import java.util.UUID

import akka.http.javadsl.server.MalformedQueryParamRejection
import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.GiftCardPassTransactionType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardPassesChargeFSpec extends GiftCardPassesFSpec {

  abstract class GiftCardPassesChargeFSpecContext extends GiftCardPassResourceFSpecContext

  "POST /v1/gift_card_passes.charge?gift_card_pass_id=$&amount=$" in {
    "if request has valid token" should {
      "charge a gift card pass" in new GiftCardPassesChargeFSpecContext {
        val giftCardPass =
          Factory
            .giftCardPass(giftCard, orderItem, originalAmount = Some(20), balanceAmount = Some(11))
            .create
        val transaction1 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-5)).create
        val transaction2 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-4)).create

        val amount = 10

        Post(s"/v1/gift_card_passes.charge?gift_card_pass_id=${giftCardPass.id}&amount=$amount")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[GiftCardPassTransaction]].data
          entity.total.amount ==== amount
          entity.total.currency ==== currency
          entity.`type` ==== GiftCardPassTransactionType.Payment

          entity.pass should beSome
          val updatedPass = entity.pass.get
          updatedPass.originalBalance.amount ==== giftCardPass.originalAmount
          updatedPass.balance.amount ==== giftCardPass.balanceAmount - amount
          updatedPass.updatedAt !=== giftCardPass.updatedAt

          val maybeGiftCardPassRecord = giftCardPassDao.findById(giftCardPass.id).await
          maybeGiftCardPassRecord should beSome
          val giftCardPassRecord = maybeGiftCardPassRecord.get
          giftCardPassRecord.originalAmount ==== giftCardPass.originalAmount
          giftCardPassRecord.balanceAmount ==== giftCardPass.balanceAmount - amount
          giftCardPassRecord.updatedAt !=== giftCardPass.updatedAt
        }
      }

      "if the gift card pass does not exist return 404" in new GiftCardPassesChargeFSpecContext {
        val amount = 10

        Post(s"/v1/gift_card_passes.charge?gift_card_pass_id=${UUID.randomUUID}&amount=$amount")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }

      "if amount is bigger than the balance" in new GiftCardPassesChargeFSpecContext {
        val giftCardPass =
          Factory
            .giftCardPass(giftCard, orderItem, originalAmount = Some(20), balanceAmount = Some(11))
            .create
        val transaction1 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-5)).create
        val transaction2 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-4)).create

        val amount = 1000

        Post(s"/v1/gift_card_passes.charge?gift_card_pass_id=${giftCardPass.id}&amount=$amount")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }

      "if amount is negative" in new GiftCardPassesChargeFSpecContext {
        val giftCardPass =
          Factory
            .giftCardPass(giftCard, orderItem, originalAmount = Some(20), balanceAmount = Some(11))
            .create
        val transaction1 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-5)).create
        val transaction2 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-4)).create

        val amount = -10

        Post(s"/v1/gift_card_passes.charge?gift_card_pass_id=${giftCardPass.id}&amount=$amount")
          .addHeader(authorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[MalformedQueryParamRejection]
        }
      }

      "if the gift card pass does not belong to the merchant" should {
        "return 404" in new GiftCardPassesChargeFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGiftCardProduct = Factory.giftCardProduct(competitor).create
          val competitorGiftCard = Factory.giftCard(competitorGiftCardProduct).create
          val competitorOrder = Factory.order(competitor).create
          val competitorOrderItem = Factory.orderItem(competitorOrder).create

          val competitorGiftCardPass =
            Factory.giftCardPass(competitorGiftCard, competitorOrderItem).create

          Post(s"/v1/gift_card_passes.charge?gift_card_pass_id=${competitorGiftCardPass.id}&amount=10")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }

}
