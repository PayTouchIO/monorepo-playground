package io.paytouch.core.resources

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.Ids
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.{ NoFilters, SessionFilters, SupplierFilters }
import io.paytouch.core.services.AuthenticationService

trait SessionResource extends JsonResource {

  def authenticationService: AuthenticationService

  val sessionRoutes: Route =
    path("sessions.list") {
      get {
        parameters("oauth2_app_name".?) { oauthAppName =>
          paginateWithDefaults(30) { implicit pagination =>
            authenticate { implicit user =>
              val filters = SessionFilters(user.id, oauthAppName)
              onSuccess(authenticationService.findAll(filters)(NoExpansions())) {
                case result =>
                  completeAsPaginatedApiResponse(result)
              }
            }
          }
        }
      }
    } ~
      path("sessions.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(authenticationService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      }
}
