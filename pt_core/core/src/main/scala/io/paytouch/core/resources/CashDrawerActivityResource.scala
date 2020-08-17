package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.CashDrawerActivityUpsertion
import io.paytouch.core.expansions.{ CashDrawerActivityExpansions, NoExpansions }
import io.paytouch.core.filters.CashDrawerActivityFilters
import io.paytouch.core.services.CashDrawerActivityService
import io.paytouch.core.utils.Multiple

trait CashDrawerActivityResource extends JsonResource {

  def cashDrawerActivityService: CashDrawerActivityService

  val cashDrawerActivityRoutes: Route =
    path("cash_drawer_activities.list") {
      get {
        paginateWithDefaults(30) { implicit pagination =>
          parameters("cash_drawer_id".as[UUID], "updated_since".as[ZonedDateTime].?).as(CashDrawerActivityFilters) {
            filters =>
              authenticate { implicit user =>
                expandParameters("user_infos")(CashDrawerActivityExpansions.apply) { expansions =>
                  onSuccess(cashDrawerActivityService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
          }
        }
      }
    } ~
      path("cash_drawer_activities.sync") {
        post {
          parameter("cash_drawer_activity_id".as[UUID]) { id =>
            entity(as[CashDrawerActivityUpsertion]) { upsertion =>
              authenticate { implicit user =>
                onSuccess(cashDrawerActivityService.syncById(id, upsertion)) {
                  case result =>
                    completeAsApiResponse(Multiple.success(result))
                }
              }
            }
          }
        }
      }
}
