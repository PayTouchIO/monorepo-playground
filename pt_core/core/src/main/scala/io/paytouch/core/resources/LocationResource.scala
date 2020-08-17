package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities._
import io.paytouch.core.expansions.LocationExpansions
import io.paytouch.core.filters.LocationFilters
import io.paytouch.core.services.LocationService

trait LocationResource extends JsonResource {
  def locationService: LocationService

  val locationRoutes: Route =
    concat(
      path("locations.create") {
        post {
          parameters("location_id".as[UUID]) { id =>
            entity(as[LocationCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(locationService.create(id, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
      path("locations.deep_copy") {
        post {
          parameters("from".as[UUID], "to".as[UUID]) { (from, to) =>
            authenticate { implicit user =>
              onSuccess(locationService.deepCopy(from, to))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("locations.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(locationService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("locations.get") {
        get {
          parameter("location_id".as[UUID]) { id =>
            expandParameters("settings", "tax_rates", "opening_hours")(LocationExpansions.apply) { expansions =>
              userOrAppAuthenticate { implicit user =>
                onSuccess(locationService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      },
      path("locations.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("q".?, "all".as[Boolean].?) {
              case (query, showAll) =>
                expandParameters("settings", "tax_rates", "opening_hours")(LocationExpansions.apply) { expansions =>
                  userOrAppAuthenticate { implicit user =>
                    val filters = LocationFilters(user.locationIds, query, showAll)
                    onSuccess(locationService.findAll(filters)(expansions)) { (locations, count) =>
                      completeAsPaginatedApiResponse(locations, count)
                    }
                  }
                }
            }
          }
        }
      },
      path("locations.update") {
        post {
          parameter("location_id".as[UUID]) { id =>
            entity(as[LocationUpdate]) { update =>
              authenticate { implicit user =>
                // initial order number cannot be updated
                onSuccess(locationService.update(id, update.copy(initialOrderNumber = None))) { result =>
                  completeAsApiResponse(result)
                }
              }
            }
          }
        }
      },
      path("locations.update_settings") {
        post {
          parameter("location_id".as[UUID]) { id =>
            entity(as[LocationSettingsUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(locationService.updateSettings(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
    )
}
