package io.paytouch.core.resources.tickets

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TicketsUpdateFSpec extends TicketsFSpec {

  abstract class TicketsUpdateFSpecContext extends TicketResourceFSpecContext

  "POST /v1/order_routing_tickets.update" in {
    "if request has valid token" in {
      "update a ticket" in new TicketsUpdateFSpecContext {
        val order = Factory.order(merchant, Some(london)).create
        val orderItem1 = Factory.orderItem(order).create
        val orderItem2 = Factory.orderItem(order).create
        val orderItem3 = Factory.orderItem(order).create

        val orderItems = Seq(orderItem1, orderItem2, orderItem3)

        val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

        val update = TicketUpdate(
          locationId = Some(london.id),
          orderId = Some(order.id),
          orderItemIds = Some(orderItems.map(_.id)),
          status = Some(TicketStatus.InProgress),
          show = Some(true),
          routeToKitchenId = None,
        )

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val ticket = responseAs[ApiResponse[Ticket]].data
          assertUpdate(ticket.id, update)
          assertResponseById(ticket.id, ticket, orderItems, Some(order))
        }
      }

      "update a ticket from new to completed should fill both start/complete at dates" in new TicketsUpdateFSpecContext {
        val order = Factory.order(merchant, Some(london)).create
        val ticket = Factory.ticket(order, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create

        val update = TicketUpdate.empty.copy(status = Some(TicketStatus.Completed))

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val ticket = responseAs[ApiResponse[Ticket]].data
          assertUpdate(ticket.id, update)
          assertResponseById(ticket.id, ticket, Seq.empty, Some(order))
          ticket.startedAt must beSome
          ticket.completedAt must beSome
          ticket.startedAt ==== ticket.completedAt
        }
      }

      "update a ticket with a subset of order items" in new TicketsUpdateFSpecContext {
        val order = Factory.order(merchant, Some(london)).create
        val orderItem1 = Factory.orderItem(order).create
        val orderItem2 = Factory.orderItem(order).create
        val orderItem3 = Factory.orderItem(order).create

        val orderItems = Seq(orderItem1, orderItem2)

        val ticket = Factory.ticket(order, routeToKitchenId = kitchen.id).create

        val update = TicketUpdate(
          locationId = Some(london.id),
          orderId = Some(order.id),
          orderItemIds = Some(orderItems.map(_.id)),
          status = Some(TicketStatus.InProgress),
          show = Some(true),
          routeToKitchenId = None,
        )

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val ticket = responseAs[ApiResponse[Ticket]].data
          assertUpdate(ticket.id, update)
          assertResponseById(ticket.id, ticket, orderItems, Some(order))
        }
      }

      "reject update if order items are empty" in new TicketsUpdateFSpecContext {
        val order = Factory.order(merchant, Some(london)).create
        val ticket = Factory.ticket(order, routeToKitchenId = kitchen.id).create

        val update = TicketUpdate.empty.copy(orderItemIds = Some(Seq.empty))

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }

      "reject update if order does not belong to the given location" in new TicketsUpdateFSpecContext {
        val order = Factory.order(merchant, Some(rome)).create
        val orderItem1 = Factory.orderItem(order).create
        val orderItem2 = Factory.orderItem(order).create
        val orderItem3 = Factory.orderItem(order).create

        val ticket = Factory.ticket(order, routeToKitchenId = kitchen.id).create

        val update =
          TicketUpdate.empty.copy(locationId = Some(london.id))

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }

      "reject update if user does not have access to the given location" in new TicketsUpdateFSpecContext {
        val newYork = Factory.location(merchant).create

        val order = Factory.order(merchant, Some(newYork)).create
        val orderItem1 = Factory.orderItem(order).create
        val orderItem2 = Factory.orderItem(order).create
        val orderItem3 = Factory.orderItem(order).create

        val ticket = Factory.ticket(order, routeToKitchenId = kitchen.id).create

        val update = TicketUpdate.empty.copy(locationId = Some(newYork.id))

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("NonAccessibleLocationIds")
        }
      }

      "reject update if order items do not belong to the given order" in new TicketsUpdateFSpecContext {

        val orderA = Factory.order(merchant, Some(london)).create
        val orderB = Factory.order(merchant, Some(london)).create
        val orderItem1 = Factory.orderItem(orderB).create
        val orderItem2 = Factory.orderItem(orderB).create
        val orderItem3 = Factory.orderItem(orderB).create

        val ticket = Factory.ticket(orderA, routeToKitchenId = kitchen.id).create

        val update = TicketUpdate(
          locationId = None,
          orderId = None,
          orderItemIds = Some(Seq(orderItem1.id, orderItem2.id, orderItem3.id)),
          status = None,
          show = None,
          routeToKitchenId = None,
        )

        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=${ticket.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TicketsUpdateFSpecContext {
        val randomUuid = UUID.randomUUID
        val update = random[TicketUpdate]
        Post(s"/v1/order_routing_tickets.update?order_routing_ticket_id=$randomUuid", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
