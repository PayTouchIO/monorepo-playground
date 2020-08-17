package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.entities.{ Ids, InventoryCountCreation, InventoryCountUpdate }
import io.paytouch.core.expansions.{ InventoryCountExpansions, InventoryCountProductExpansions }
import io.paytouch.core.filters.{ InventoryCountFilters, InventoryCountProductFilters }
import io.paytouch.core.services.{ InventoryCountProductService, InventoryCountService }

trait InventoryCountResource extends JsonResource { self: CommentResource =>

  def inventoryCountService: InventoryCountService
  def inventoryCountProductService: InventoryCountProductService

  lazy val inventoryCountRoutes: Route =
    path("inventory_counts.create") {
      post {
        parameters("inventory_count_id".as[UUID]) { id =>
          entity(as[InventoryCountCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(inventoryCountService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("inventory_counts.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(inventoryCountService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("inventory_counts.get") {
        get {
          parameter("inventory_count_id".as[UUID]) { inventoryCountId =>
            expandParameters("user", "location")(InventoryCountExpansions.apply) { expansions =>
              authenticate { implicit user =>
                onSuccess(inventoryCountService.findById(inventoryCountId)(expansions)) { result =>
                  completeAsOptApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("inventory_counts.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "q".?,
              "status".as[InventoryCountStatus].?,
            ).as(InventoryCountFilters) { filters =>
              expandParameters("user", "location")(InventoryCountExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(inventoryCountService.findAll(filters)(expansions)) { (inventoryCounts, count) =>
                    completeAsPaginatedApiResponse(inventoryCounts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("inventory_counts.list_products") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("inventory_count_id".as[UUID]).as(InventoryCountProductFilters) { filters =>
              expandParameters("options")(InventoryCountProductExpansions) { expansions =>
                authenticate { implicit user =>
                  onSuccess(inventoryCountProductService.findAll(filters)(expansions)) {
                    (inventoryCountProducts, count) => completeAsPaginatedApiResponse(inventoryCountProducts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("inventory_counts.sync_inventory") {
        post {
          parameter("inventory_count_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(inventoryCountService.syncInventoryById(id))(result => completeAsApiResponse(result))
            }
          }
        }
      } ~
      path("inventory_counts.update") {
        post {
          parameter("inventory_count_id".as[UUID]) { id =>
            entity(as[InventoryCountUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(inventoryCountService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~ commentRoutes(inventoryCountService, "inventory_counts", "inventory_count_id")
}
