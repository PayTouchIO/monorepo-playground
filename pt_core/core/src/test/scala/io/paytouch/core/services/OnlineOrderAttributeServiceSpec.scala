package io.paytouch.core.services

import scala.concurrent._

import cats.implicits._

import com.softwaremill.macwire._

import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.model.enums.{ AcceptanceStatus, OrderStatus, Source }
import io.paytouch.core.data.model.StatusTransition
import io.paytouch.core.entities.{ OrderAcception, OrderRejection, ReceiptContext, Store }
import io.paytouch.core.expansions.OrderExpansions
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class OnlineOrderAttributeServiceSpec extends ServiceDaoSpec {
  abstract class OrderServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    override val merchantService = mock[MerchantService]
    val service: OnlineOrderAttributeService = wire[OnlineOrderAttributeService]

    lazy val statusTransitions = Seq(StatusTransition(status = OrderStatus.Completed))
    lazy val globalCustomer = Factory.globalCustomerWithEmail(Some(merchant), email = Some(email)).create

    lazy val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.Pending
    lazy val onlineOrderAttribute = Factory.onlineOrderAttribute(merchant, Some(acceptanceStatus)).create

    lazy val storefrontOrder =
      Factory
        .order(
          merchant,
          location = Some(rome),
          onlineOrderAttribute = Some(onlineOrderAttribute),
          globalCustomer = Some(globalCustomer),
          statusTransitions = Some(statusTransitions),
          source = Some(Source.Storefront),
        )
        .create

    // Delivery order has no customer
    lazy val deliveryOrder =
      Factory
        .order(
          merchant,
          location = Some(rome),
          onlineOrderAttribute = Some(onlineOrderAttribute),
          statusTransitions = Some(statusTransitions),
          source = Some(Source.DeliveryProvider),
        )
        .create

    @scala.annotation.nowarn("msg=Auto-application")
    lazy val receiptContext = random[ReceiptContext]
    merchantService.prepareReceiptContext(any, any)(any) returns receiptContext.some.pure[Future]

    lazy val store = Store(rome.id, Seq.empty)
  }

  "OnlineOrderAttributeService" in {
    "accept" should {
      "if successful" should {
        "send the correct message for source = storefront" in new OrderServiceSpecContext {
          val orderAcception = OrderAcception(Some(randomInt))
          service.acceptAndThenSendAcceptanceStatusMessage(storefrontOrder.id, orderAcception).await.success

          val orderEntity =
            orderService
              .findById(storefrontOrder.id, orderService.defaultFilters)(OrderExpansions.withFullOrderItems)
              .await
              .get

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderAcceptedEmail(receiptContext)))
        }

        "send the correct message for source = delivery_provider" in new OrderServiceSpecContext {
          val orderAcception = OrderAcception(Some(randomInt))
          service.acceptAndThenSendAcceptanceStatusMessage(deliveryOrder.id, orderAcception).await.success

          val orderEntity =
            orderService
              .findById(deliveryOrder.id, orderService.defaultFilters)(OrderExpansions.withFullOrderItems)
              .await
              .get

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(DeliveryOrderAccepted(orderEntity)))
        }
      }

      "if validation fails" should {
        "not send any message" in new OrderServiceSpecContext {
          val orderAcception = OrderAcception(Some(randomInt))
          override lazy val acceptanceStatus = AcceptanceStatus.Rejected
          service.accept(storefrontOrder.id, orderAcception).await.failures

          actorMock.expectNoMessage()
        }
      }
    }

    "reject" should {
      "if successful" should {
        "send the correct message for source = storefront" in new OrderServiceSpecContext {
          val orderRejection = OrderRejection(Some(randomWord))
          service.reject(storefrontOrder.id, orderRejection).await.success

          val orderEntity =
            orderService
              .findById(storefrontOrder.id, orderService.defaultFilters)(OrderExpansions.withFullOrderItems)
              .await
              .get

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderRejectedEmail(receiptContext)))
        }

        "send the correct message for source = delivery_provider" in new OrderServiceSpecContext {
          val orderRejection = OrderRejection(Some(randomWord))
          service.reject(deliveryOrder.id, orderRejection).await.success

          val orderEntity =
            orderService
              .findById(deliveryOrder.id, orderService.defaultFilters)(OrderExpansions.withFullOrderItems)
              .await
              .get

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(DeliveryOrderRejected(orderEntity)))
        }
      }

      "if validation fails" should {
        "not send any message" in new OrderServiceSpecContext {
          override lazy val acceptanceStatus = AcceptanceStatus.Accepted

          val orderRejection = OrderRejection(Some(randomWord))
          service.reject(storefrontOrder.id, orderRejection).await.failures

          actorMock.expectNoMessage()
        }
      }
    }
  }
}
