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

class TipsAssignmentServiceSpec extends ServiceDaoSpec {

  abstract class TipsAssignmentServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    implicit val logger = new PaytouchLogger
    val service: TipsAssignmentService = wire[TipsAssignmentService]

    @scala.annotation.nowarn("msg=Auto-application")
    val upsertion = random[TipsAssignmentUpsertion].copy(locationId = london.id)
  }

  "TipsAssignmentService" in {
    "syncById" should {
      "send EntitySynced[TipsAssignment] messages" in new TipsAssignmentServiceSpecContext {
        val newId = UUID.randomUUID
        val (_, entity: TipsAssignment) = service.syncById(newId, upsertion)(userCtx).await

        val msg = EntitySynced[IdOnlyEntity](IdOnlyEntity(entity.id, entity.classShortName), london.id)(userCtx)
        actorMock.expectMsg(SendMsgWithRetry(msg))
        actorMock.expectNoMessage()
      }
    }
  }
}
