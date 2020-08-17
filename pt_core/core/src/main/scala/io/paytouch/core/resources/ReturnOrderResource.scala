package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.ReturnOrderStatus
import io.paytouch.core.entities.{ ReturnOrderCreation, ReturnOrderUpdate }
import io.paytouch.core.expansions.{ ReturnOrderExpansions, ReturnOrderProductExpansions }
import io.paytouch.core.filters.{ ReturnOrderFilters, ReturnOrderProductFilters }
import io.paytouch.core.services.{ ReturnOrderProductService, ReturnOrderService }

trait ReturnOrderResource extends JsonResource { self: CommentResource =>

  def returnOrderService: ReturnOrderService
  def returnOrderProductService: ReturnOrderProductService

  lazy val returnOrderRoutes: Route =
    path("return_orders.create") {
      post {
        parameters("return_order_id".as[UUID]) { id =>
          entity(as[ReturnOrderCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(returnOrderService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("return_orders.get") {
        get {
          parameter("return_order_id".as[UUID]) { id =>
            expandParameters("supplier", "user", "location", "products_count", "purchase_order")(
              ReturnOrderExpansions.forGet,
            ) { expansions =>
              authenticate { implicit user =>
                onSuccess(returnOrderService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("return_orders.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
              "status".as[ReturnOrderStatus].?,
            ).as(ReturnOrderFilters) { filters =>
              expandParameters("supplier", "user", "stock_value", "location", "products_count", "purchase_order")(
                ReturnOrderExpansions.forList,
              ) { expansions =>
                authenticate { implicit user =>
                  onSuccess(returnOrderService.findAll(filters)(expansions)) { (returnOrders, count) =>
                    completeAsPaginatedApiResponse(returnOrders, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("return_orders.list_products") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("return_order_id".as[UUID]).as(ReturnOrderProductFilters) { filters =>
              expandParameters("options")(ReturnOrderProductExpansions) { expansions =>
                authenticate { implicit user =>
                  onSuccess(returnOrderProductService.findAll(filters)(expansions)) { (returnOrderProducts, count) =>
                    completeAsPaginatedApiResponse(returnOrderProducts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("return_orders.sync_inventory") {
        post {
          parameter("return_order_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(returnOrderService.syncInventoryById(id))(result => completeAsApiResponse(result))
            }
          }
        }
      } ~
      path("return_orders.update") {
        post {
          parameter("return_order_id".as[UUID]) { id =>
            entity(as[ReturnOrderUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(returnOrderService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~ commentRoutes(returnOrderService, "return_orders", "return_order_id")
}
