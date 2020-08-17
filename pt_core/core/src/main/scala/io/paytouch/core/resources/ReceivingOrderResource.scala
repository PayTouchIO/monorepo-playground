package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.ReceivingOrderStatus
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ ReceivingOrderExpansions, ReceivingOrderProductExpansions }
import io.paytouch.core.filters.{ ReceivingOrderFilters, ReceivingOrderProductFilters }
import io.paytouch.core.services.{ ReceivingOrderProductService, ReceivingOrderService }

trait ReceivingOrderResource extends JsonResource { self: CommentResource =>

  def receivingOrderService: ReceivingOrderService
  def receivingOrderProductService: ReceivingOrderProductService

  lazy val receivingOrderRoutes: Route =
    path("receiving_orders.create") {
      post {
        parameters("receiving_order_id".as[UUID]) { id =>
          entity(as[ReceivingOrderCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(receivingOrderService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("receiving_orders.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(receivingOrderService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("receiving_orders.get") {
        get {
          parameter("receiving_order_id".as[UUID]) { id =>
            expandParameters("location", "purchase_order", "transfer_order", "user")(ReceivingOrderExpansions.forGet) {
              expansions =>
                authenticate { implicit user =>
                  onSuccess(receivingOrderService.findById(id)(expansions)) { result =>
                    completeAsOptApiResponse(result)
                  }
                }
            }
          }
        }
      } ~
      path("receiving_orders.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
              "status".as[ReceivingOrderStatus].?,
            ).as(ReceivingOrderFilters) { filters =>
              expandParameters("products_count", "stock_value", "user", "purchase_order", "transfer_order")(
                ReceivingOrderExpansions.forList,
              ) { expansions =>
                authenticate { implicit user =>
                  onSuccess(receivingOrderService.findAll(filters)(expansions)) { (receivingOrders, count) =>
                    completeAsPaginatedApiResponse(receivingOrders, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("receiving_orders.list_products") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("receiving_order_id".as[UUID]).as(ReceivingOrderProductFilters) { filters =>
              expandParameters("options")(ReceivingOrderProductExpansions) { expansions =>
                authenticate { implicit user =>
                  onSuccess(receivingOrderProductService.findAll(filters)(expansions)) {
                    (receivingOrderProducts, count) => completeAsPaginatedApiResponse(receivingOrderProducts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("receiving_orders.sync_inventory") {
        post {
          parameter("receiving_order_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(receivingOrderService.syncInventoryById(id))(result => completeAsApiResponse(result))
            }
          }
        }
      } ~
      path("receiving_orders.update") {
        post {
          parameter("receiving_order_id".as[UUID]) { id =>
            entity(as[ReceivingOrderUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(receivingOrderService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      commentRoutes(receivingOrderService, "receiving_orders", "receiving_order_id")
}
