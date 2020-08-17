package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities._
import io.paytouch.core.expansions.CatalogExpansions
import io.paytouch.core.filters._
import io.paytouch.core.services.CatalogService

trait CatalogResource extends JsonResource {
  def catalogService: CatalogService

  val catalogRoutes: Route =
    path("catalogs.create") {
      post {
        parameters("catalog_id".as[UUID]) { id =>
          entity(as[CatalogCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(catalogService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("catalogs.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(catalogService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("catalogs.get") {
        get {
          parameter("catalog_id".as[UUID]) { catalogId =>
            expandParameters("products_count", "categories_count", "availabilities", "location_overrides")(
              CatalogExpansions.apply,
            ) { expansions =>
              userOrAppAuthenticate { implicit user =>
                onSuccess(catalogService.findById(catalogId)(expansions)) { result =>
                  completeAsOptApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("catalogs.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameter(
              "id[]".as[Seq[UUID]].?,
            ) { ids =>
              val filters = CatalogFilters(ids)

              expandParameters("products_count", "categories_count", "availabilities", "location_overrides")(
                CatalogExpansions.apply,
              ) { expansions =>
                userOrAppAuthenticate { implicit user =>
                  onSuccess(catalogService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("catalogs.update") {
        post {
          parameter("catalog_id".as[UUID]) { id =>
            entity(as[CatalogUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(catalogService.update(id, update.asUpsertion))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
