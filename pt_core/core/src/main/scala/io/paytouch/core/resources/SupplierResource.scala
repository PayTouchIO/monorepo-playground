package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.expansions.SupplierExpansions
import io.paytouch.core.filters.SupplierFilters
import io.paytouch.core.services.SupplierService

trait SupplierResource extends JsonResource {
  def supplierService: SupplierService

  val supplierRoutes: Route =
    path("suppliers.create") {
      post {
        parameters("supplier_id".as[UUID]) { id =>
          entity(as[SupplierCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(supplierService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("suppliers.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(supplierService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("suppliers.get") {
        get {
          parameter("supplier_id".as[UUID]) { supplierId =>
            expandParameters("locations", "products_count", "stock_values")(SupplierExpansions.apply) { expansions =>
              authenticate { implicit user =>
                onSuccess(supplierService.findById(supplierId)(expansions)) { result =>
                  completeAsOptApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("suppliers.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "location_id[]".as[Seq[UUID]].?,
              "category_id".as[UUID].?,
              "category_id[]".as[Seq[UUID]].?,
              "q".?,
            ) { (locationId, locationIds, categoryId, categoryIds, q) =>
              val filters =
                SupplierFilters.forList(
                  locationIds.combineWithOne(locationId),
                  categoryId,
                  categoryIds,
                  q,
                )

              expandParameters(
                "locations",
                "products_count",
                "stock_values",
              )(SupplierExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(supplierService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("suppliers.update") {
        post {
          parameter("supplier_id".as[UUID]) { id =>
            entity(as[SupplierUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(supplierService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("suppliers.update_active") {
        parameters("supplier_id".as[UUID]) { id =>
          post {
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(supplierService.updateActiveLocations(id, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      }
}
