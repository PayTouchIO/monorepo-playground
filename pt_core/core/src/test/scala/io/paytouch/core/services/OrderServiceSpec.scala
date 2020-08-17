package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._

import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.entities.SendReceiptData
import io.paytouch.core.expansions.OrderExpansions
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, PaytouchLogger }
import io.paytouch.utils.Tagging._

class OrderServiceSpec extends ServiceDaoSpec {
  abstract class OrderServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    implicit val paytouchLogger: PaytouchLogger = wire[PaytouchLogger]
    val _orderSyncService: OrderSyncService = wire[OrderSyncService]
    val service: OrderService = wire[OrderService]
    val order = Factory.orderWithStatusTransitions(merchant, rome).create
    val paymentTransactionA = Factory.paymentTransaction(order).create
    val paymentTransactionB = Factory.paymentTransaction(order).create
    val recipientEmail = randomEmail
    val data = SendReceiptData(recipientEmail)
  }

  "OrderService" in {
    "rejectOrder" should {
      "send order updated message" in new OrderServiceSpecContext {
        service.rejectOrder(order).await

        val orderEntity =
          service.findById(order.id, service.defaultFilters)(OrderExpansions.withFullOrderItems).await.get

        actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
        actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
      }
    }
    "sendReceipt" should {
      "if successful" should {
        "send the correct message with no payment transaction id" in new OrderServiceSpecContext {
          service.sendReceipt(order.id, None, data).await.success

          val orderEntity =
            service.findById(order.id, service.defaultFilters)(OrderExpansions.withFullOrderItems).await.get

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(PrepareOrderReceipt(orderEntity, None, recipientEmail)))
        }

        "send the correct message with payment transaction id" in new OrderServiceSpecContext {
          service.sendReceipt(order.id, Some(paymentTransactionA.id), data).await.success

          val orderEntity =
            service.findById(order.id, service.defaultFilters)(OrderExpansions.withFullOrderItems).await.get

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          actorMock.expectMsg(
            SendMsgWithRetry(PrepareOrderReceipt(orderEntity, Some(paymentTransactionA.id), recipientEmail)),
          )
        }
      }

      "if validation fails" should {
        "not send any message" in new OrderServiceSpecContext {
          service.sendReceipt(UUID.randomUUID, None, data).await.failures

          actorMock.expectNoMessage()
        }
      }
    }
  }
}
