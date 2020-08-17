package io.paytouch.core.resources

import java.util.UUID
import java.time.LocalDateTime

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.TimeOffCardFilters
import io.paytouch.core.services.TimeOffCardService

trait TimeOffCardResource extends JsonResource {

  def timeOffCardService: TimeOffCardService

  val timeOffCardsRoutes: Route =
    path("time_off_cards.create") {
      post {
        parameter("time_off_card_id".as[UUID]) { id =>
          entity(as[TimeOffCardCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(timeOffCardService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("time_off_cards.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(timeOffCardService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("time_off_cards.get") {
        get {
          parameter("time_off_card_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(timeOffCardService.findById(id)(NoExpansions()))(result => completeAsOptApiResponse(result))
            }
          }
        }
      } ~
      path("time_off_cards.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameter("location_id".as[UUID].?, "from".as[LocalDateTime].?, "to".as[LocalDateTime].?, "q".?) {
              case (locationId, from, to, q) =>
                authenticate { implicit user =>
                  val filters = TimeOffCardFilters.withAccessibleLocations(locationId, q, from, to)
                  onSuccess(timeOffCardService.findAll(filters)(NoExpansions())) { (records, count) =>
                    completeAsPaginatedApiResponse(records, count)
                  }
                }
            }
          }
        }
      } ~
      path("time_off_cards.update") {
        post {
          parameter("time_off_card_id".as[UUID]) { id =>
            entity(as[TimeOffCardUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(timeOffCardService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
