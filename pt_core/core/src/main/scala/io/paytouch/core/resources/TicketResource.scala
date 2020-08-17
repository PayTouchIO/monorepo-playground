package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.{ MalformedQueryParamRejection, Route }
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.entities.{ TicketCreation, TicketUpdate }
import io.paytouch.core.expansions.TicketExpansions
import io.paytouch.core.filters.TicketFilters
import io.paytouch.core.services.{ KitchenService, TicketService }

trait TicketResource extends JsonResource {

  def ticketService: TicketService
  def kitchenService: KitchenService // TODO: remove me when we are done

  lazy val ticketRoutes: Route =
    path("order_routing_tickets.create") {
      post {
        parameter("order_routing_ticket_id".as[UUID]) { id =>
          entity(as[TicketCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(ticketService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("order_routing_tickets.get") {
        get {
          parameter("order_routing_ticket_id".as[UUID]) { id =>
            authenticate { implicit user =>
              expandParameters("order")(TicketExpansions.apply) { expansions =>
                onSuccess(ticketService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("order_routing_tickets.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameter(
              "route_to_kitchen_id".as[UUID],
              "location_id".as[UUID].?,
              "order_number".?,
              "show".as[Boolean].?,
              "status".as[TicketStatus].?,
            ) {
              case (routeToKitchenId, locationId, orderNumber, show, status) =>
                expandParameters("order")(TicketExpansions.apply) { expansions =>
                  authenticate { implicit user =>
                    val filters = TicketFilters.withAccessibleLocations(
                      Seq(routeToKitchenId),
                      locationId,
                      orderNumber,
                      show,
                      status,
                    )
                    onSuccess(ticketService.findAll(filters)(expansions)) { (tickets, count) =>
                      completeAsPaginatedApiResponse(tickets, count)
                    }
                  }
                }
            }
          }
        }
      } ~
      path("order_routing_tickets.update") {
        post {
          parameter("order_routing_ticket_id".as[UUID]) { id =>
            entity(as[TicketUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(ticketService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
