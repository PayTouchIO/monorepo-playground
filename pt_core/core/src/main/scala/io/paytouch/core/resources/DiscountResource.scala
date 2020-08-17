package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.expansions.DiscountExpansions
import io.paytouch.core.filters.DiscountFilters
import io.paytouch.core.services.DiscountService

trait DiscountResource extends JsonResource {
  def discountService: DiscountService

  val discountRoutes: Route =
    path("discounts.create") {
      post {
        parameters("discount_id".as[UUID]) { id =>
          entity(as[DiscountCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(discountService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("discounts.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(discountService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("discounts.get") {
        get {
          parameter("discount_id".as[UUID]) { id =>
            authenticate { implicit user =>
              expandParameters("locations", "availabilities")(DiscountExpansions.apply) { expansions =>
                onSuccess(discountService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("discounts.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "location_id[]".as[Seq[UUID]].?,
              "q".?,
              "updated_since".as[ZonedDateTime].?,
            ) { (locationId, locationIds, q, updatedSince) =>
              val filters =
                DiscountFilters(
                  locationIds.combineWithOne(locationId),
                  q,
                  updatedSince,
                )

              expandParameters("locations", "availabilities")(DiscountExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(discountService.findAll(filters)(expansions)) { (discounts, count) =>
                    completeAsPaginatedApiResponse(discounts, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("discounts.update") {
        post {
          parameter("discount_id".as[UUID]) { id =>
            entity(as[DiscountUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(discountService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("discounts.update_active") {
        parameters("discount_id".as[UUID]) { id =>
          post {
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(discountService.updateActiveLocations(id, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      }
}
