package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.monitors.{ ReceivingOrderChange, ReceivingOrderMonitor }
import io.paytouch.core.entities._
import io.paytouch.utils.Tagging._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReceivingOrderServiceSpec extends ServiceDaoSpec {

  abstract class ReceivingOrdersServiceSpecContext extends ServiceDaoSpecContext {
    val receivingOrderMonitor = actorMock.ref.taggedWith[ReceivingOrderMonitor]

    val service = wire[ReceivingOrderService]

    @scala.annotation.nowarn("msg=Auto-application")
    val validCreation = random[ReceivingOrderCreation]
      .copy(locationId = rome.id, receivingObjectId = None, receivingObjectType = None, products = Seq.empty)
    val validUpdate = validCreation.asUpdate
  }

  "ReceivingOrderService" in {
    "create" should {
      "if successful" should {
        "not send any messages" in new ReceivingOrdersServiceSpecContext {
          val newId = UUID.randomUUID
          val (_, receivingOrderEntity) = service.create(newId, validCreation).await.success

          val state = None
          actorMock.expectMsg(ReceivingOrderChange(state, receivingOrderEntity, userCtx))
          actorMock.expectNoMessage()
        }
      }

      "if validation fails" should {
        "not send any message" in new ReceivingOrdersServiceSpecContext {
          val newId = UUID.randomUUID
          val creation = validCreation.copy(locationId = UUID.randomUUID)

          service.create(newId, creation).await.failures

          actorMock.expectNoMessage()
        }
      }
    }
    "update" should {
      "if successful" should {
        "send the correct messages" in new ReceivingOrdersServiceSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          val (_, receivingOrderEntity) = service.update(receivingOrder.id, validUpdate).await.success

          val state = (receivingOrder, Seq.empty)
          actorMock.expectMsg(ReceivingOrderChange(Some(state), receivingOrderEntity, userCtx))
          actorMock.expectNoMessage()
        }
      }

      "if validation fails" should {
        "not send any message" in new ReceivingOrdersServiceSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          val update = validUpdate.copy(locationId = Some(UUID.randomUUID))

          service.update(receivingOrder.id, update).await.failures

          actorMock.expectNoMessage()
        }
      }
    }
  }
}
