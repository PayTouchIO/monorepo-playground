package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.{ Ids, UserRoleCreation, UserRoleUpdate }
import io.paytouch.core.expansions.UserRoleExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.UserRoleService

trait UserRoleResource extends JsonResource {

  def userRoleService: UserRoleService

  val userRoleRoutes: Route =
    path("user_roles.create") {
      post {
        parameter("user_role_id".as[UUID]) { id =>
          entity(as[UserRoleCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(userRoleService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("user_roles.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(userRoleService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("user_roles.get") {
        get {
          parameter("user_role_id".as[UUID]) { userRoleId =>
            expandParameters("users_count")(UserRoleExpansions.withPermissions) { expansions =>
              authenticate { implicit user =>
                onSuccess(userRoleService.findById(userRoleId)(expansions)) { result =>
                  completeAsOptApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("user_roles.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            expandParameters("users_count")(UserRoleExpansions.withoutPermissions) { expansions =>
              authenticate { implicit user =>
                onSuccess(userRoleService.findAll(NoFilters())(expansions)) { (userRoles, count) =>
                  completeAsPaginatedApiResponse(userRoles, count)
                }
              }
            }
          }
        }
      } ~
      path("user_roles.update") {
        post {
          parameter("user_role_id".as[UUID]) { id =>
            entity(as[UserRoleUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(userRoleService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
