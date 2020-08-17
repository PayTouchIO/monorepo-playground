package io.paytouch.core.resources

import java.time.ZonedDateTime

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.EventFilters
import io.paytouch.core.services.EventService

trait EventResource extends JsonResource {

  def eventService: EventService

  val eventRoutes: Route =
    path("changes.feed") {
      get {
        paginateWithDefaults(30) { implicit pagination =>
          parameters(
            "updated_since".as[ZonedDateTime],
            "action".as[TrackableAction].?,
            "object".as[ExposedName].?,
          ).as(EventFilters) { filters =>
            authenticate { implicit user =>
              onSuccess(eventService.findAll(filters)(NoExpansions())) {
                case result =>
                  completeAsPaginatedApiResponse(result)
              }
            }
          }
        }
      }
    }
}
