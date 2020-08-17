package io.paytouch.core.resources

import java.time.LocalDate
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.ShiftStatus
import io.paytouch.core.entities._
import io.paytouch.core.expansions.ShiftExpansions
import io.paytouch.core.filters.ShiftFilters
import io.paytouch.core.services.ShiftService

trait ShiftResource extends JsonResource {

  def shiftService: ShiftService

  val shiftRoutes: Route =
    path("shifts.create") {
      post {
        parameter("shift_id".as[UUID]) { id =>
          entity(as[ShiftCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(shiftService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("shifts.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(shiftService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("shifts.get") {
        get {
          parameter("shift_id".as[UUID]) { id =>
            expandParameters("locations")(ShiftExpansions) { expansions =>
              authenticate { implicit user =>
                onSuccess(shiftService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("shifts.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "from".as[LocalDate].?,
              "to".as[LocalDate].?,
              "user_role_id".as[UUID].?,
              "status".as[ShiftStatus].?,
            ) {
              case (locationId, from, to, userRoleId, status) =>
                expandParameters("locations")(ShiftExpansions) { expansions =>
                  authenticate { implicit user =>
                    val filters = ShiftFilters.withAccessibleLocations(locationId, from, to, userRoleId, status)
                    onSuccess(shiftService.findAll(filters)(expansions)) { (shifts, count) =>
                      completeAsPaginatedApiResponse(shifts, count)
                    }
                  }
                }
            }
          }
        }
      } ~
      path("shifts.update") {
        post {
          parameter("shift_id".as[UUID]) { shiftId =>
            entity(as[ShiftUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(shiftService.update(shiftId, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
