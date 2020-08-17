package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.TimeCardStatus
import io.paytouch.core.expansions.TimeCardExpansions
import io.paytouch.core.filters.TimeCardFilters
import io.paytouch.core.services.TimeCardService

trait TimeCardResource extends JsonResource {

  def timeCardService: TimeCardService

  val timeCardRoutes: Route =
    path("time_cards.clock") {
      post {
        entity(as[TimeCardClock]) { clockData =>
          authenticate { implicit user =>
            onSuccess(timeCardService.clock(clockData))(result => completeAsApiResponse(result))
          }
        }
      }
    } ~ path("time_cards.create") {
      post {
        parameter("time_card_id".as[UUID]) { id =>
          entity(as[TimeCardCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(timeCardService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("time_cards.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(timeCardService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("time_cards.get") {
        get {
          parameter("time_card_id".as[UUID]) { timeCardId =>
            expandParameters("shift")(TimeCardExpansions) { expansions =>
              authenticate { implicit user =>
                onSuccess(timeCardService.findById(timeCardId)(expansions)) { result =>
                  completeAsOptApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("time_cards.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameter(
              "location_id".as[UUID].?,
              "from".as[LocalDateTime].?,
              "to".as[LocalDateTime].?,
              "status".as[TimeCardStatus].?,
              "q".?,
            ) {
              case (locationId, from, to, status, q) =>
                expandParameters("shift")(TimeCardExpansions) { expansions =>
                  authenticate { implicit user =>
                    val filters = TimeCardFilters.withAccessibleLocations(locationId, from, to, status, q)
                    onSuccess(timeCardService.findAll(filters)(expansions)) { (timeCards, count) =>
                      completeAsPaginatedApiResponse(timeCards, count)
                    }
                  }
                }
            }
          }
        }
      } ~
      path("time_cards.update") {
        post {
          parameter("time_card_id".as[UUID]) { timeCardId =>
            entity(as[TimeCardUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(timeCardService.update(timeCardId, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
