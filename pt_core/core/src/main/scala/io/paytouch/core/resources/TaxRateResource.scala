package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.expansions.TaxRateExpansions
import io.paytouch.core.filters.TaxRateFilters
import io.paytouch.core.services.TaxRateService

trait TaxRateResource extends JsonResource {
  def taxRateService: TaxRateService

  val taxRateRoutes: Route =
    path("tax_rates.create") {
      post {
        parameters("tax_rate_id".as[UUID]) { id =>
          entity(as[TaxRateCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(taxRateService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("tax_rates.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(taxRateService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("tax_rates.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameter(
              "location_id".as[UUID].?,
              "location_id[]".as[Seq[UUID]].?,
              "updated_since".as[ZonedDateTime].?,
            ) { (locationId, locationIds, updatedSince) =>
              val filters =
                TaxRateFilters(
                  locationIds.combineWithOne(locationId),
                  updatedSince,
                )

              expandParameters("locations")(TaxRateExpansions) { expansions =>
                authenticate { implicit user =>
                  onSuccess(taxRateService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("tax_rates.update") {
        post {
          parameter("tax_rate_id".as[UUID]) { id =>
            entity(as[TaxRateUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(taxRateService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("tax_rates.update_active") {
        parameters("tax_rate_id".as[UUID]) { id =>
          post {
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(taxRateService.updateActiveLocations(id, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      }
}
