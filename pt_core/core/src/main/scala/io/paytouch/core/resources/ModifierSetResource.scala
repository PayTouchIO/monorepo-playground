package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.expansions.ModifierSetExpansions
import io.paytouch.core.filters.ModifierSetFilters
import io.paytouch.core.services.ModifierSetService

trait ModifierSetResource extends JsonResource {
  def modifierSetService: ModifierSetService

  val modifierSetRoutes: Route =
    path("modifier_sets.assign_products") {
      post {
        parameter("modifier_set_id".as[UUID]) { id =>
          entity(as[ModifierSetProductsAssignment]) { productsAssignment =>
            authenticate { implicit user =>
              onSuccess(modifierSetService.assignProducts(id, productsAssignment)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
    } ~
      path("modifier_sets.create") {
        post {
          parameter("modifier_set_id".as[UUID]) { id =>
            entity(as[ModifierSetCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(modifierSetService.create(id, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("modifier_sets.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(modifierSetService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("modifier_sets.get") {
        get {
          parameter("modifier_set_id".as[UUID]) { id =>
            expandParameters("products_count", "locations")(ModifierSetExpansions.apply) { expansions =>
              authenticate { implicit user =>
                onSuccess(modifierSetService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("modifier_sets.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "location_id[]".as[Seq[UUID]].?,
              "id[]".as[Seq[UUID]].?,
              "q".?,
              "updated_since".as[ZonedDateTime].?,
            ) { (locationId, locationIds, ids, q, updatedSince) =>
              val filters =
                ModifierSetFilters(
                  ids,
                  locationIds.combineWithOne(locationId),
                  q,
                  updatedSince,
                )

              expandParameters("products_count", "locations")(ModifierSetExpansions.apply) { expansions =>
                userOrAppAuthenticate { implicit user =>
                  onSuccess(modifierSetService.findAll(filters)(expansions)) { (modifierSets, count) =>
                    completeAsPaginatedApiResponse(modifierSets, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("modifier_sets.update") {
        post {
          parameter("modifier_set_id".as[UUID]) { id =>
            entity(as[ModifierSetUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(modifierSetService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("modifier_sets.update_active") {
        parameters("modifier_set_id".as[UUID]) { id =>
          post {
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(modifierSetService.updateActiveLocations(id, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      }
}
