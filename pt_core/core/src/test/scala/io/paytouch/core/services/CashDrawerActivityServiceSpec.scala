package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.model.enums.{ OrderStatus, PaymentStatus, Source }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities._
import io.paytouch.core.utils.{ PaytouchLogger, FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class CashDrawerActivityServiceSpec extends ServiceDaoSpec {

  abstract class CashDrawerActivityServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    implicit val logger = new PaytouchLogger
    val service: CashDrawerActivityService = wire[CashDrawerActivityService]

    val cashDrawer = Factory.cashDrawer(user, rome).create
    val cashDrawerActivityId = UUID.randomUUID

    val upsertion = random[CashDrawerActivityUpsertion].copy(cashDrawerId = cashDrawer.id)
  }

  "CashDrawerActivityService" in {
    "syncById" should {
      "send EntitySynced[CashDrawerActivity] messages" in new CashDrawerActivityServiceSpecContext {
        val newId = UUID.randomUUID
        val (_, entity: CashDrawerActivity) = service.syncById(newId, upsertion)(userCtx).await

        val msg = EntitySynced[IdOnlyEntity](IdOnlyEntity(entity.id, entity.classShortName), rome.id)(userCtx)
        actorMock.expectMsg(SendMsgWithRetry(msg))
        actorMock.expectNoMessage()
      }
    }
  }
}
