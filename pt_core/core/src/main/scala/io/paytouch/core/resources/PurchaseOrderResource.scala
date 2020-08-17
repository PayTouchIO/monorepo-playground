package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities.enums.ReceivingOrderView
import io.paytouch.core.entities.{ Ids, PurchaseOrderCreation, PurchaseOrderUpdate }
import io.paytouch.core.expansions.{ PurchaseOrderExpansions, PurchaseOrderProductExpansions }
import io.paytouch.core.filters.{ PurchaseOrderFilters, PurchaseOrderProductFilters }
import io.paytouch.core.services.{ PurchaseOrderProductService, PurchaseOrderService }

trait PurchaseOrderResource extends JsonResource { self: CommentResource =>

  def purchaseOrderService: PurchaseOrderService
  def purchaseOrderProductService: PurchaseOrderProductService

  lazy val purchaseOrderRoutes: Route =
    path("purchase_orders.create") {
      post {
        parameters("purchase_order_id".as[UUID]) { id =>
          entity(as[PurchaseOrderCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(purchaseOrderService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("purchase_orders.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(purchaseOrderService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("purchase_orders.get") {
        get {
          parameter("purchase_order_id".as[UUID]) { purchaseOrderId =>
            expandParameters("receiving_orders", "supplier", "location", "user")(PurchaseOrderExpansions.forGet) {
              expansions =>
                authenticate { implicit user =>
                  onSuccess(purchaseOrderService.findById(purchaseOrderId)(expansions)) { result =>
                    completeAsOptApiResponse(result)
                  }
                }
            }
          }
        }
      } ~
      path("purchase_orders.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "supplier_id".as[UUID].?,
              "status".as[ReceivingObjectStatus].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
              "view".as[ReceivingOrderView].?,
            ).as(PurchaseOrderFilters) { filters =>
              expandParameters(
                "receiving_orders",
                "supplier",
                "ordered_products_count",
                "received_products_count",
                "returned_products_count",
              )(PurchaseOrderExpansions.forList) { expansions =>
                authenticate { implicit user =>
                  onSuccess(purchaseOrderService.findAll(filters)(expansions)) { (purchaseOrders, count) =>
                    completeAsPaginatedApiResponse(purchaseOrders, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("purchase_orders.list_products") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("purchase_order_id".as[UUID]).as(PurchaseOrderProductFilters) { filters =>
              expandParameters("options")(PurchaseOrderProductExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(purchaseOrderProductService.findAll(filters)(expansions)) {
                    (purchaseOrderProducts, count) => completeAsPaginatedApiResponse(purchaseOrderProducts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("purchase_orders.update") {
        post {
          parameter("purchase_order_id".as[UUID]) { id =>
            entity(as[PurchaseOrderUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(purchaseOrderService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("purchase_orders.send") {
        post {
          parameter("purchase_order_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(purchaseOrderService.send(id))(result => completeAsApiResponse(result))
            }
          }
        }
      } ~ commentRoutes(purchaseOrderService, "purchase_orders", "purchase_order_id")
}
