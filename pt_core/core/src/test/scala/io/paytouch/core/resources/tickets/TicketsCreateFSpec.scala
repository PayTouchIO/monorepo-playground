package io.paytouch.core.resources.tickets

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TicketsCreateFSpec extends TicketsFSpec {

  abstract class TicketsCreateFSpecContext extends TicketResourceFSpecContext

  "POST /v1/order_routing_tickets.create" in {
    "if request has valid token" in {
      "without route_to_kitchen_id" in {
        "reject creation" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val orderItems = Seq(orderItem1, orderItem2, orderItem3)

          val creation = TicketCreation(
            locationId = london.id,
            orderId = order.id,
            orderItemIds = orderItems.map(_.id),
            routeToKitchenId = None,
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("MatchingKitchenIdNotFound")
          }
        }
      }

      "with route_to_kitchen_id" in {
        "create a ticket" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val orderItems = Seq(orderItem1, orderItem2, orderItem3)

          val creation = TicketCreation(
            locationId = london.id,
            orderId = order.id,
            orderItemIds = orderItems.map(_.id),
            routeToKitchenId = Some(kitchen.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val ticket = responseAs[ApiResponse[Ticket]].data
            assertCreation(newTicketId, creation)
            assertResponseById(newTicketId, ticket, orderItems, Some(order))
          }
        }

        "create a ticket if kitchen has kds_enabled=false should autocomplete ticket" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val orderItems = Seq(orderItem1, orderItem2, orderItem3)

          val creation = TicketCreation(
            locationId = london.id,
            orderId = order.id,
            orderItemIds = orderItems.map(_.id),
            routeToKitchenId = Some(kitchenKdsDisabled.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val ticket = responseAs[ApiResponse[Ticket]].data
            assertUpdate(newTicketId, creation.asUpdate.copy(status = Some(TicketStatus.Completed)))
            assertResponseById(newTicketId, ticket, orderItems, Some(order))
          }
        }

        "create a ticket if the kitchen has been deleted" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val orderItems = Seq(orderItem1, orderItem2, orderItem3)

          val creation = TicketCreation(
            locationId = london.id,
            orderId = order.id,
            orderItemIds = orderItems.map(_.id),
            routeToKitchenId = Some(deletedKitchen.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val ticket = responseAs[ApiResponse[Ticket]].data
            assertCreation(newTicketId, creation)
            assertResponseById(newTicketId, ticket, orderItems, Some(order))
          }
        }

        "reject creation if order items are empty" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(london)).create

          val creation = TicketCreation(
            locationId = london.id,
            orderId = order.id,
            orderItemIds = Seq.empty,
            routeToKitchenId = Some(kitchen.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("InvalidTicketOrderItemsAssociation")
          }
        }

        "reject creation if order does not belong to the given location" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(rome)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val creation = TicketCreation(
            locationId = london.id,
            orderId = order.id,
            orderItemIds = Seq(orderItem1.id, orderItem2.id, orderItem3.id),
            routeToKitchenId = Some(kitchen.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidOrderLocationAssociation")
          }
        }

        "reject creation if kitchen does not belong to the given location" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val order = Factory.order(merchant, Some(rome)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val creation = TicketCreation(
            locationId = rome.id,
            orderId = order.id,
            orderItemIds = Seq(orderItem1.id, orderItem2.id, orderItem3.id),
            routeToKitchenId = Some(bar.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("NonAccessibleLocationIds", "KitchenLocationIdMismatch")
          }
        }

        "reject creation if user does not have access to the given location" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val newYork = Factory.location(merchant).create

          val order = Factory.order(merchant, Some(newYork)).create
          val orderItem1 = Factory.orderItem(order).create
          val orderItem2 = Factory.orderItem(order).create
          val orderItem3 = Factory.orderItem(order).create

          val creation = TicketCreation(
            locationId = newYork.id,
            orderId = order.id,
            orderItemIds = Seq(orderItem1.id, orderItem2.id, orderItem3.id),
            routeToKitchenId = Some(kitchen.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("NonAccessibleLocationIds", "NonAccessibleKitchenIds")
          }
        }

        "reject creation if order items do not belong to the given order" in new TicketsCreateFSpecContext {
          val newTicketId = UUID.randomUUID

          val orderA = Factory.order(merchant, Some(london)).create
          val orderB = Factory.order(merchant, Some(london)).create
          val orderItem1 = Factory.orderItem(orderB).create
          val orderItem2 = Factory.orderItem(orderB).create
          val orderItem3 = Factory.orderItem(orderB).create

          val creation = TicketCreation(
            locationId = london.id,
            orderId = orderA.id,
            orderItemIds = Seq(orderItem1.id, orderItem2.id, orderItem3.id),
            routeToKitchenId = Some(kitchen.id),
          )

          Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$newTicketId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("InvalidOrderOrderItemsAssociation")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TicketsCreateFSpecContext {
        val randomUuid = UUID.randomUUID
        val creation = random[TicketCreation]
        Post(s"/v1/order_routing_tickets.create?order_routing_ticket_id=$randomUuid", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
