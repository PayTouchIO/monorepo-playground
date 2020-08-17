package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities.enums.ReceivingOrderView
import io.paytouch.core.entities.{ TransferOrderCreation, TransferOrderUpdate }
import io.paytouch.core.expansions.{ TransferOrderExpansions, TransferOrderProductExpansions }
import io.paytouch.core.filters.{ TransferOrderFilters, TransferOrderProductFilters }
import io.paytouch.core.services.{ TransferOrderProductService, TransferOrderService }

trait TransferOrderResource extends JsonResource {

  def transferOrderService: TransferOrderService
  val transferOrderProductService: TransferOrderProductService

  lazy val transferOrderRoutes: Route =
    path("transfer_orders.create") {
      post {
        parameters("transfer_order_id".as[UUID]) { id =>
          entity(as[TransferOrderCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(transferOrderService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("transfer_orders.get") {
        get {
          parameter("transfer_order_id".as[UUID]) { id =>
            expandParameters("from_location", "to_location", "user")(TransferOrderExpansions.forGet) { expansions =>
              authenticate { implicit user =>
                onSuccess(transferOrderService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("transfer_orders.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "status".as[ReceivingObjectStatus].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
              "view".as[ReceivingOrderView].?,
            ).as(TransferOrderFilters) { filters =>
              expandParameters("from_location", "to_location", "user", "products_count", "stock_value")(
                TransferOrderExpansions.forList,
              ) { expansions =>
                authenticate { implicit user =>
                  onSuccess(transferOrderService.findAll(filters)(expansions)) { (transferOrders, count) =>
                    completeAsPaginatedApiResponse(transferOrders, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("transfer_orders.list_products") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("transfer_order_id".as[UUID]).as(TransferOrderProductFilters) { filters =>
              expandParameters("options")(TransferOrderProductExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(transferOrderProductService.findAll(filters)(expansions)) {
                    (transferOrderProducts, count) => completeAsPaginatedApiResponse(transferOrderProducts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("transfer_orders.update") {
        post {
          parameter("transfer_order_id".as[UUID]) { id =>
            entity(as[TransferOrderUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(transferOrderService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
