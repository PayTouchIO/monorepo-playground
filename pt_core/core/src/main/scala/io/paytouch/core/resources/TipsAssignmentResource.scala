package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities.enums.HandledVia
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.TipsAssignmentFilters
import io.paytouch.core.services.TipsAssignmentService
import io.paytouch.core.utils.Multiple

trait TipsAssignmentResource extends JsonResource {
  def tipsAssignmentService: TipsAssignmentService

  val tipsAssignmentsRoutes: Route =
    concat(
      path("tips_assignments.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID],
              "handled_via".as[HandledVia].?,
              "updated_since".as[ZonedDateTime].?,
            ) { (locationId, handledVia, updatedSince) =>
              authenticate { implicit user =>
                val filters = TipsAssignmentFilters.withAccessibleLocations(locationId, handledVia, updatedSince)
                onSuccess(tipsAssignmentService.findAll(filters)(NoExpansions())) {
                  case result =>
                    completeAsPaginatedApiResponse(result)
                }
              }
            }
          }
        }
      },
      path("tips_assignments.sync") {
        post {
          parameter("tips_assignment_id".as[UUID]) { id =>
            entity(as[TipsAssignmentUpsertion]) { upsertion =>
              authenticate { implicit user =>
                onSuccess(tipsAssignmentService.syncById(id, upsertion)) {
                  case result =>
                    completeAsApiResponse(Multiple.success(result))
                }
              }
            }
          }
        }
      },
      path("tips_assignments.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(tipsAssignmentService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
    )
}
