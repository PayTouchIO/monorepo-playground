package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities.CashDrawerUpsertion
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.CashDrawerFilters
import io.paytouch.core.services.CashDrawerService
import io.paytouch.core.utils.Multiple

trait CashDrawerResource extends JsonResource {
  def cashDrawerService: CashDrawerService

  val cashDrawerRoutes: Route =
    path("cash_drawers.list") {
      get {
        paginateWithDefaults(30) { implicit pagination =>
          parameters(
            "location_id".as[UUID].?,
            "updated_since".as[ZonedDateTime].?,
          ) {
            case (locationId, updatedSince) =>
              authenticate { implicit user =>
                val filters = CashDrawerFilters.withAccessibleLocations(locationId, updatedSince)
                onSuccess(cashDrawerService.findAll(filters)(NoExpansions())) {
                  case result =>
                    completeAsPaginatedApiResponse(result)
                }
              }
          }
        }
      }
    } ~
      path("cash_drawers.get") {
        get {
          parameter("cash_drawer_id".as[UUID]) { cashDrawerId =>
            authenticate { implicit user =>
              onSuccess(cashDrawerService.findById(cashDrawerId)(NoExpansions())) { result =>
                completeAsOptApiResponse(result)
              }
            }
          }
        }
      } ~
      path("cash_drawers.sync") {
        post {
          parameter("cash_drawer_id".as[UUID]) { id =>
            entity(as[CashDrawerUpsertion]) { upsertion =>
              authenticate { implicit user =>
                onSuccess(cashDrawerService.syncById(id, upsertion)) {
                  case result =>
                    completeAsApiResponse(Multiple.success(result))
                }
              }
            }
          }
        }
      } ~
      path("cash_drawers.list_reasons") {
        get {
          authenticate { implicit user =>
            onSuccess(cashDrawerService.listReasons()) {
              case result =>
                completeSeqAsApiResponse(result)
            }
          }
        }
      } ~
      path("cash_drawers.send_report") {
        post {
          parameter("cash_drawer_id".as[UUID]) { cashDrawerId =>
            authenticate { implicit user =>
              onSuccess(cashDrawerService.sendReport(cashDrawerId))(result => completeAsEmptyResponse(result))
            }
          }
        }
      }
}
