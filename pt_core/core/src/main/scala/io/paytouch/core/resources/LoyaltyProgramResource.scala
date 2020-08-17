package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch.core.entities.{ LoyaltyProgramCreation, LoyaltyProgramUpdate }
import io.paytouch.core.expansions.LoyaltyProgramExpansions
import io.paytouch.core.filters.LoyaltyProgramFilters
import io.paytouch.core.services.LoyaltyProgramService

trait LoyaltyProgramResource extends JsonResource {
  def loyaltyProgramService: LoyaltyProgramService

  val loyaltyProgramRoutes: Route =
    path("loyalty_programs.create") {
      post {
        parameters("loyalty_program_id".as[UUID]) { id =>
          entity(as[LoyaltyProgramCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(loyaltyProgramService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("loyalty_programs.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "updated_since".as[ZonedDateTime].?,
            ).as(LoyaltyProgramFilters) { filters =>
              expandParameters("locations")(LoyaltyProgramExpansions.apply) { expansions =>
                authenticate { implicit user =>
                  onSuccess(loyaltyProgramService.findAll(filters)(expansions)) { (loyaltyPrograms, count) =>
                    completeAsPaginatedApiResponse(loyaltyPrograms, count)
                  }
                }
              }
            }
          }
        }
      } ~
      path("loyalty_programs.update") {
        post {
          parameter("loyalty_program_id".as[UUID]) { id =>
            entity(as[LoyaltyProgramUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(loyaltyProgramService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
