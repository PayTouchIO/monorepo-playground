package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.{ Ids, KitchenCreation, KitchenUpdate }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.{ KitchenFilters, NoFilters }
import io.paytouch.core.services.KitchenService

trait KitchenResource extends JsonResource {

  def kitchenService: KitchenService

  val kitchenRoutes: Route =
    path("kitchens.create") {
      post {
        parameters("kitchen_id".as[UUID]) { id =>
          entity(as[KitchenCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(kitchenService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("kitchens.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("location_id".as[UUID].?, "updated_since".as[ZonedDateTime].?) { (locationId, updatedSince) =>
              authenticate { implicit user =>
                val filters = KitchenFilters.withAccessibleLocations(locationId, updatedSince)
                onSuccess(kitchenService.findAll(filters)(NoExpansions())) {
                  case result =>
                    completeAsPaginatedApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("kitchens.update") {
        post {
          parameter("kitchen_id".as[UUID]) { id =>
            entity(as[KitchenUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(kitchenService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("kitchens.get") {
        get {
          parameter("kitchen_id".as[UUID]) { id =>
            entity(as[KitchenUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(kitchenService.findById(id)(NoExpansions()))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("kitchens.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(kitchenService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      }
}
