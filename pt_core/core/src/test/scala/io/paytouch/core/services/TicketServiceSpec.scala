package io.paytouch.core.services

import java.util.UUID

import akka.actor.{ ActorRef, Props }

import com.softwaremill.macwire._

import io.paytouch.core.async.monitors.TicketMonitor
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.model.enums.OrderStatus
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class TicketServiceSpec extends ServiceDaoSpec {
  abstract class TicketsServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    val ticketMonitor: ActorRef withTag TicketMonitor =
      actorSystem.actorOf(Props(new TicketMonitor(messageHandler))).taggedWith[TicketMonitor]

    val service = wire[TicketService]

    val order = Factory.order(merchant, Some(london), status = Some(OrderStatus.Received)).create
    val orderItem1 = Factory.orderItem(order).create
    val orderItem2 = Factory.orderItem(order).create
    val orderItem3 = Factory.orderItem(order).create

    val orderItems = Seq(orderItem1, orderItem2, orderItem3)

    val kitchen = Factory.kitchen(london).create

    val orderDao = daos.orderDao
  }

  "TicketService" in {
    "update" should {
      "if successful" should {
        "order items handling" should {
          "send the correct messages for all new order items" in new TicketsServiceSpecContext {
            val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

            val update = TicketUpdate(
              locationId = Some(london.id),
              orderId = Some(order.id),
              orderItemIds = Some(orderItems.map(_.id)),
              status = Some(TicketStatus.InProgress),
              show = Some(true),
              routeToKitchenId = None,
            )

            val (_, ticketEntity) = service.update(ticket.id, update).await.success

            actorMock.expectMsgAllOf(
              SendMsgWithRetry(TicketUpdatedV2(ticketEntity)(userContext)),
              SendMsgWithRetry(OrderChanged.updated(ticketEntity.order.get)(userContext)),
              SendMsgWithRetry(OrderItemUpdated(ticketEntity.orderItems(0), london.id)(userContext)),
              SendMsgWithRetry(OrderItemUpdated(ticketEntity.orderItems(1), london.id)(userContext)),
              SendMsgWithRetry(OrderItemUpdated(ticketEntity.orderItems(2), london.id)(userContext)),
            )

            actorMock.expectNoMessage()
          }

          "send the messages for order items that have changed" in new TicketsServiceSpecContext {
            val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

            Factory.ticketOrderItem(ticket, orderItem1).create
            Factory.ticketOrderItem(ticket, orderItem2).create

            val update = TicketUpdate(
              locationId = Some(london.id),
              orderId = Some(order.id),
              orderItemIds = Some(orderItems.map(_.id)),
              status = None,
              show = Some(true),
              routeToKitchenId = None,
            )

            val (_, ticketEntity) = service.update(ticket.id, update).await.success

            actorMock.expectMsgAllOf(
              SendMsgWithRetry(TicketUpdatedV2(ticketEntity)(userContext)),
              SendMsgWithRetry(OrderChanged.updated(ticketEntity.order.get)(userContext)),
              SendMsgWithRetry(
                OrderItemUpdated(ticketEntity.orderItems.find(_.id == orderItem3.id).get, london.id)(userContext),
              ),
            )

            actorMock.expectNoMessage()
          }

          "do not send the messages if order items didn't change" in new TicketsServiceSpecContext {
            val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

            Factory.ticketOrderItem(ticket, orderItem1).create
            Factory.ticketOrderItem(ticket, orderItem2).create
            Factory.ticketOrderItem(ticket, orderItem3).create

            val update = TicketUpdate(
              locationId = Some(london.id),
              orderId = Some(order.id),
              orderItemIds = Some(orderItems.map(_.id)),
              status = None,
              show = Some(true),
              routeToKitchenId = None,
            )

            val (_, ticketEntity) = service.update(ticket.id, update).await.success

            actorMock.expectMsgAllOf(
              SendMsgWithRetry(TicketUpdatedV2(ticketEntity)(userContext)),
              SendMsgWithRetry(OrderChanged.updated(ticketEntity.order.get)(userContext)),
            )

            actorMock.expectNoMessage()
          }
        }

        "if validation fails" should {
          "not send any message" in new TicketsServiceSpecContext {
            val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

            val update = random[TicketUpdate].copy(orderItemIds = Some(Seq(UUID.randomUUID)))

            service.update(ticket.id, update).await.failures

            actorMock.expectNoMessage()
          }
        }
      }

      "eventually complete the order if all the tickets are marked as completed" should {
        class AutoCompleteSpecContext extends TicketsServiceSpecContext {
          val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create
          val ticketCompleted =
            Factory.ticket(order, status = Some(TicketStatus.Completed), routeToKitchenId = kitchen.id).create

          val update = TicketUpdate(
            locationId = Some(london.id),
            orderId = Some(order.id),
            orderItemIds = Some(orderItems.map(_.id)),
            status = Some(TicketStatus.Completed),
            show = Some(true),
            routeToKitchenId = None,
          )
        }

        "if location_settings.order_autocomplete=true" in new AutoCompleteSpecContext {
          Factory.locationSettings(london, orderAutocomplete = Some(true)).create
          service.update(ticket.id, update).await.success

          afterAWhile {
            val updatedOrder = orderDao.findById(order.id).await.get
            updatedOrder.status ==== Some(OrderStatus.Completed)
            updatedOrder.completedAt must beSome
            updatedOrder.completedAtTz must beSome
          }
        }

        "if location_settings.order_autocomplete=false" in new AutoCompleteSpecContext {
          Factory.locationSettings(london, orderAutocomplete = Some(false)).create

          val (_, ticketEntity) = service.update(ticket.id, update.copy(orderItemIds = None)).await.success

          orderDao.findById(order.id).await.get.status ==== order.status
          actorMock.expectMsgAllOf(
            SendMsgWithRetry(TicketUpdatedV2(ticketEntity)(userContext)),
            SendMsgWithRetry(OrderChanged.updated(ticketEntity.order.get)(userContext)),
          )
          actorMock.expectNoMessage()
        }
      }
    }

    "create" should {
      "if successful" should {
        "send the correct messages for all order items" in new TicketsServiceSpecContext {
          val newId = UUID.randomUUID
          val creation =
            TicketCreation(
              locationId = london.id,
              orderId = order.id,
              orderItemIds = orderItems.map(_.id),
              routeToKitchenId = Some(kitchen.id),
            )

          val (_, ticketEntity) = service.create(newId, creation).await.success

          actorMock.expectMsgAllOf(
            SendMsgWithRetry(TicketCreatedV2(ticketEntity)(userContext)),
            SendMsgWithRetry(OrderChanged.updated(ticketEntity.order.get)(userContext)),
            SendMsgWithRetry(OrderItemUpdated(ticketEntity.orderItems(0), london.id)(userContext)),
            SendMsgWithRetry(OrderItemUpdated(ticketEntity.orderItems(1), london.id)(userContext)),
            SendMsgWithRetry(OrderItemUpdated(ticketEntity.orderItems(2), london.id)(userContext)),
          )

          actorMock.expectNoMessage()
        }
      }

      "if validation fails" should {
        "not send any message" in new TicketsServiceSpecContext {
          val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

          val creation = random[TicketCreation]

          service.create(ticket.id, creation).await.failures

          actorMock.expectNoMessage()
        }
      }
    }
  }
}
