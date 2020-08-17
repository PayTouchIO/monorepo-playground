package io.paytouch.core.resources.tickets

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TicketsGetFSpec extends TicketsFSpec {

  abstract class TicketsGetFSpecContext extends TicketResourceFSpecContext

  "GET /v1/order_routing_tickets.get" in {
    "if request has valid token" in {

      "if the discount does not belong to the merchant" should {
        "with no expansions" should {
          "return the ticket" in new TicketsGetFSpecContext {
            val order = Factory.order(merchant, Some(london)).create
            val orderItem1 = Factory.orderItem(order).create
            val orderItem2 = Factory.orderItem(order).create
            val orderItem3 = Factory.orderItem(order).create

            val ticket = Factory.ticket(order, routeToKitchenId = kitchen.id).create
            val ticketOrderItem1 = Factory.ticketOrderItem(ticket, orderItem1).create
            val ticketOrderItem2 = Factory.ticketOrderItem(ticket, orderItem2).create

            Get(s"/v1/order_routing_tickets.get?order_routing_ticket_id=${ticket.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Ticket]].data

              assertResponse(entity, ticket, Seq(orderItem1, orderItem2))
            }
          }
        }

        "with expand[]=order" should {
          "return the ticket" in new TicketsGetFSpecContext {
            val order = Factory.order(merchant, Some(london)).create
            val orderItem1 = Factory.orderItem(order).create
            val orderItem2 = Factory.orderItem(order).create
            val orderItem3 = Factory.orderItem(order).create

            val ticket = Factory.ticket(order, routeToKitchenId = kitchen.id).create
            val ticketOrderItem1 = Factory.ticketOrderItem(ticket, orderItem1).create
            val ticketOrderItem2 = Factory.ticketOrderItem(ticket, orderItem2).create

            Get(s"/v1/order_routing_tickets.get?order_routing_ticket_id=${ticket.id}&expand[]=order")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[Ticket]].data

              assertResponse(entity, ticket, Seq(orderItem1, orderItem2), order = Some(order))
            }
          }
        }
      }

      "if the ticket does not belong to the merchant" should {
        "return 404" in new TicketsGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          val competitorOrder = Factory.order(competitor, Some(competitorLocation)).create
          Factory.orderItem(competitorOrder).create

          val competitorTicket = Factory.ticket(competitorOrder, routeToKitchenId = kitchen.id).create

          Get(s"/v1/order_routing_tickets.get?order_routing_ticket_id=${competitorTicket.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
