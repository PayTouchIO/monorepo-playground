package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.entities.{ GiftCardCreation, GiftCardUpdate }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities.GiftCardChanged
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class GiftCardServiceSpec extends ServiceDaoSpec {

  abstract class GiftCardServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val service = wire[GiftCardService]
  }

  "GiftCardService" in {
    "create" should {
      "create gift card and call monitor" in new GiftCardServiceSpecContext {
        val randomUuid = UUID.randomUUID
        val creation = random[GiftCardCreation]
        val (resultType, entity) = service.create(randomUuid, creation).await.success

        actorMock.expectMsg(SendMsgWithRetry(GiftCardChanged(merchant.id, entity)))
      }
    }

    "update" should {
      "update gift card and call monitor" in new GiftCardServiceSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct).create
        val update = random[GiftCardUpdate]
        val (resultType, entity) = service.update(giftCard.id, update).await.success

        actorMock.expectMsg(SendMsgWithRetry(GiftCardChanged(merchant.id, entity)))
      }
    }
  }
}
