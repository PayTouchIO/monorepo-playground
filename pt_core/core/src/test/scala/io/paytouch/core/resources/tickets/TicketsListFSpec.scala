package io.paytouch.core.resources.tickets

import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TicketsListFSpec extends TicketsFSpec {

  abstract class TicketsListFSpecContext extends TicketResourceFSpecContext

  "GET /v1/order_routing_tickets.list" in {
    "if request has valid token" in {

      "with no parameters" should {
        "reject the request" in new TicketsListFSpecContext {

          Get(s"/v1/order_routing_tickets.list").addHeader(authorizationHeader) ~> routes ~> check {
            rejection should beAnInstanceOf[MissingQueryParamRejection]
          }
        }
      }

      "with route_to_kitchen_id parameter" should {
        "with location_id filter" should {
          "return all the tickets" in new TicketsListFSpecContext {
            val orderA = Factory.order(merchant, Some(london)).create
            val orderItemA1 = Factory.orderItem(orderA).create
            val orderItemA2 = Factory.orderItem(orderA).create
            val orderItemA3 = Factory.orderItem(orderA).create
            val ticketA1 = Factory.ticket(orderA, routeToKitchenId = kitchen.id).create
            val ticketA2 = Factory.ticket(orderA, routeToKitchenId = kitchen.id).create
            val ticketA1OrderItemA1 = Factory.ticketOrderItem(ticketA1, orderItemA1).create
            val ticketA1OrderItemA2 = Factory.ticketOrderItem(ticketA1, orderItemA2).create
            val ticketA2OrderItemA1 = Factory.ticketOrderItem(ticketA2, orderItemA1).create
            val ticketA2OrderItemA3 = Factory.ticketOrderItem(ticketA2, orderItemA3).create

            val orderB = Factory.order(merchant, Some(rome)).create
            val orderItemB1 = Factory.orderItem(orderB).create
            val orderItemB2 = Factory.orderItem(orderB).create
            val orderItemB3 = Factory.orderItem(orderB).create
            val ticketB1 = Factory.ticket(orderB, routeToKitchenId = kitchen.id).create
            val ticketB2 = Factory.ticket(orderB, routeToKitchenId = kitchen.id).create
            val ticketB1OrderItemB1 = Factory.ticketOrderItem(ticketB1, orderItemB1).create
            val ticketB2OrderItemB2 = Factory.ticketOrderItem(ticketB2, orderItemB2).create

            Get(s"/v1/order_routing_tickets.list?route_to_kitchen_id=${kitchen.id}&location_id=${london.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val tickets = responseAs[PaginatedApiResponse[Seq[Ticket]]].data
              tickets.map(_.id) should containTheSameElementsAs(Seq(ticketA1.id, ticketA2.id))

              assertResponse(tickets.find(_.id == ticketA1.id).get, ticketA1, Seq(orderItemA1, orderItemA2))
              assertResponse(tickets.find(_.id == ticketA2.id).get, ticketA2, Seq(orderItemA1, orderItemA3))
            }
          }

          "return tickets with bundle data" in new TicketsListFSpecContext {
            val simple1 = Factory.simpleProduct(merchant).create
            val simple2 = Factory.simpleProduct(merchant).create

            val bundle = Factory.comboProduct(merchant).create
            val bundleSet = Factory.bundleSet(bundle).create
            val bundleOption1 = Factory.bundleOption(bundleSet, simple1).create
            val bundleOption2 = Factory.bundleOption(bundleSet, simple2).create

            val orderB = Factory.order(merchant, Some(london)).create
            val orderItemB1 = Factory.orderItem(orderB).create
            val bundleOrderItem = Factory.orderItem(orderB, product = Some(bundle)).create
            val bundleArticleOrderItem = Factory.orderItem(orderB, product = Some(simple1)).create
            val orderBundle =
              Factory
                .orderBundle(orderB, bundleOrderItem, bundleArticleOrderItem, Some(bundleSet), Some(bundleOption1))
                .create

            val ticketB1 = Factory.ticket(orderB, routeToKitchenId = kitchen.id).create
            val ticketB1OrderItemB1 = Factory.ticketOrderItem(ticketB1, orderItemB1).create

            val ticketB2 = Factory.ticket(orderB, routeToKitchenId = kitchen.id).create
            val ticketB2OrderItemB2 = Factory.ticketOrderItem(ticketB2, bundleArticleOrderItem).create

            Get(s"/v1/order_routing_tickets.list?route_to_kitchen_id=${kitchen.id}&location_id=${london.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val tickets = responseAs[PaginatedApiResponse[Seq[Ticket]]].data
              tickets.map(_.id) should containTheSameElementsAs(Seq(ticketB1.id, ticketB2.id))

              assertResponse(
                tickets.find(_.id == ticketB1.id).get,
                ticketB1,
                Seq(orderItemB1),
                bundleOrderItems = Seq(bundleOrderItem),
              )
              assertResponse(
                tickets.find(_.id == ticketB2.id).get,
                ticketB2,
                orderItems = Seq(bundleArticleOrderItem),
                bundleOrderItems = Seq(bundleOrderItem),
              )
            }
          }
        }

        "with order_number filter" should {
          "return all the tickets" in new TicketsListFSpecContext {
            val order1 = Factory.order(merchant, Some(london)).create
            val order2 = Factory.order(merchant, Some(london)).create

            val orderItem1A = Factory.orderItem(order1).create
            val orderItem1B = Factory.orderItem(order1).create
            val orderItem1C = Factory.orderItem(order1).create
            val orderItem2A = Factory.orderItem(order2).create
            val orderItem2B = Factory.orderItem(order2).create
            val orderItem2C = Factory.orderItem(order2).create

            val ticket1A = Factory.ticket(order1, routeToKitchenId = kitchen.id).create
            val ticket1B = Factory.ticket(order1, routeToKitchenId = kitchen.id).create
            val ticket2A = Factory.ticket(order2, routeToKitchenId = kitchen.id).create
            val ticket2B = Factory.ticket(order2, routeToKitchenId = kitchen.id).create

            val ticketA1OrderItemA1 = Factory.ticketOrderItem(ticket1A, orderItem1A).create
            val ticketA1OrderItemA2 = Factory.ticketOrderItem(ticket1A, orderItem1B).create
            val ticketA2OrderItemA3 = Factory.ticketOrderItem(ticket1B, orderItem1C).create
            val ticketB1OrderItemB1 = Factory.ticketOrderItem(ticket2A, orderItem2A).create
            val ticketB2OrderItemB2 = Factory.ticketOrderItem(ticket2B, orderItem2B).create

            Get(s"/v1/order_routing_tickets.list?route_to_kitchen_id=${kitchen.id}&order_number=1")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val tickets = responseAs[PaginatedApiResponse[Seq[Ticket]]].data
              tickets.map(_.id) should containTheSameElementsAs(Seq(ticket1A.id, ticket1B.id))

              assertResponse(tickets.find(_.id == ticket1A.id).get, ticket1A, Seq(orderItem1A, orderItem1B))
              assertResponse(tickets.find(_.id == ticket1B.id).get, ticket1B, Seq(orderItem1C))
            }
          }
        }

        "with show filter" should {
          "return all the tickets" in new TicketsListFSpecContext {
            val orderA = Factory.order(merchant, Some(london)).create
            val orderB = Factory.order(merchant, Some(rome)).create

            val orderItemA1 = Factory.orderItem(orderA).create
            val orderItemA2 = Factory.orderItem(orderA).create
            val orderItemA3 = Factory.orderItem(orderA).create
            val orderItemB1 = Factory.orderItem(orderB).create
            val orderItemB2 = Factory.orderItem(orderB).create
            val orderItemB3 = Factory.orderItem(orderB).create

            val ticketA1 = Factory.ticket(orderA, show = Some(true), routeToKitchenId = kitchen.id).create
            val ticketA2 = Factory.ticket(orderA, show = Some(true), routeToKitchenId = kitchen.id).create
            val ticketB1 = Factory.ticket(orderB, show = Some(false), routeToKitchenId = kitchen.id).create
            val ticketB2 = Factory.ticket(orderB, show = Some(false), routeToKitchenId = kitchen.id).create

            val ticketA1OrderItemA1 = Factory.ticketOrderItem(ticketA1, orderItemA1).create
            val ticketA1OrderItemA2 = Factory.ticketOrderItem(ticketA1, orderItemA2).create
            val ticketA2OrderItemA3 = Factory.ticketOrderItem(ticketA2, orderItemA3).create
            val ticketB1OrderItemB1 = Factory.ticketOrderItem(ticketB1, orderItemB1).create
            val ticketB2OrderItemB2 = Factory.ticketOrderItem(ticketB2, orderItemB2).create

            Get(s"/v1/order_routing_tickets.list?route_to_kitchen_id=${kitchen.id}&show=true")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val tickets = responseAs[PaginatedApiResponse[Seq[Ticket]]].data
              tickets.map(_.id) should containTheSameElementsAs(Seq(ticketA1.id, ticketA2.id))

              assertResponse(tickets.find(_.id == ticketA1.id).get, ticketA1, Seq(orderItemA1, orderItemA2))
              assertResponse(tickets.find(_.id == ticketA2.id).get, ticketA2, Seq(orderItemA3))
            }
          }
        }

        "with status filter" should {
          "return all the tickets" in new TicketsListFSpecContext {
            val orderA = Factory.order(merchant, Some(london)).create
            val orderB = Factory.order(merchant, Some(rome)).create

            val orderItemA1 = Factory.orderItem(orderA).create
            val orderItemA2 = Factory.orderItem(orderA).create
            val orderItemA3 = Factory.orderItem(orderA).create
            val orderItemB1 = Factory.orderItem(orderB).create
            val orderItemB2 = Factory.orderItem(orderB).create
            val orderItemB3 = Factory.orderItem(orderB).create

            val ticketA1 = Factory.ticket(orderA, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create
            val ticketA2 = Factory.ticket(orderA, status = Some(TicketStatus.New), routeToKitchenId = kitchen.id).create
            val ticketB1 =
              Factory.ticket(orderB, status = Some(TicketStatus.InProgress), routeToKitchenId = kitchen.id).create
            val ticketB2 =
              Factory.ticket(orderB, status = Some(TicketStatus.Completed), routeToKitchenId = kitchen.id).create

            val ticketA1OrderItemA1 = Factory.ticketOrderItem(ticketA1, orderItemA1).create
            val ticketA1OrderItemA2 = Factory.ticketOrderItem(ticketA1, orderItemA2).create
            val ticketA2OrderItemA3 = Factory.ticketOrderItem(ticketA2, orderItemA3).create
            val ticketB1OrderItemB1 = Factory.ticketOrderItem(ticketB1, orderItemB1).create
            val ticketB2OrderItemB2 = Factory.ticketOrderItem(ticketB2, orderItemB2).create

            Get(s"/v1/order_routing_tickets.list?route_to_kitchen_id=${kitchen.id}&status=new")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val tickets = responseAs[PaginatedApiResponse[Seq[Ticket]]].data
              tickets.map(_.id) should containTheSameElementsAs(Seq(ticketA1.id, ticketA2.id))

              assertResponse(tickets.find(_.id == ticketA1.id).get, ticketA1, Seq(orderItemA1, orderItemA2))
              assertResponse(tickets.find(_.id == ticketA2.id).get, ticketA2, Seq(orderItemA3))
            }
          }
        }

        "with expand[]=order" should {
          "return all the tickets" in new TicketsListFSpecContext {
            val orderA = Factory.order(merchant, Some(london)).create
            val orderB = Factory.order(merchant, Some(rome)).create

            val orderItemA1 = Factory.orderItem(orderA).create
            val orderItemA2 = Factory.orderItem(orderA).create
            val orderItemA3 = Factory.orderItem(orderA).create
            val orderItemB1 = Factory.orderItem(orderB).create
            val orderItemB2 = Factory.orderItem(orderB).create
            val orderItemB3 = Factory.orderItem(orderB).create

            val ticketA1 = Factory.ticket(orderA, routeToKitchenId = kitchen.id).create
            val ticketA2 = Factory.ticket(orderA, routeToKitchenId = kitchen.id).create
            val ticketB1 = Factory.ticket(orderB, routeToKitchenId = kitchen.id).create
            val ticketB2 = Factory.ticket(orderB, routeToKitchenId = kitchen.id).create

            val ticketA1OrderItemA1 = Factory.ticketOrderItem(ticketA1, orderItemA1).create
            val ticketA1OrderItemA2 = Factory.ticketOrderItem(ticketA1, orderItemA2).create
            val ticketA2OrderItemA3 = Factory.ticketOrderItem(ticketA2, orderItemA3).create
            val ticketB1OrderItemB1 = Factory.ticketOrderItem(ticketB1, orderItemB1).create
            val ticketB2OrderItemB2 = Factory.ticketOrderItem(ticketB2, orderItemB2).create

            Get(s"/v1/order_routing_tickets.list?route_to_kitchen_id=${kitchen.id}&expand[]=order")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val tickets = responseAs[PaginatedApiResponse[Seq[Ticket]]].data
              tickets.map(_.id) should containTheSameElementsAs(Seq(ticketA1.id, ticketA2.id, ticketB1.id, ticketB2.id))

              assertResponse(
                tickets.find(_.id == ticketA1.id).get,
                ticketA1,
                Seq(orderItemA1, orderItemA2),
                order = Some(orderA),
              )
              assertResponse(tickets.find(_.id == ticketA2.id).get, ticketA2, Seq(orderItemA3), order = Some(orderA))
              assertResponse(tickets.find(_.id == ticketB1.id).get, ticketB1, Seq(orderItemB1), order = Some(orderB))
              assertResponse(tickets.find(_.id == ticketB2.id).get, ticketB2, Seq(orderItemB2), order = Some(orderB))
            }
          }
        }

      }
    }
  }
}
