package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route

import cats.data._

import io.paytouch.core.entities._
import io.paytouch.core.errors.Errors
import io.paytouch.core.expansions.UserExpansions
import io.paytouch.core.filters.UserFilters
import io.paytouch.core.services._

trait UserResource extends JsonResource {
  def authenticationService: AuthenticationService
  val userService: UserService
  val passwordResetService: PasswordResetService

  val userRoutes: Route =
    concat(
      path("users.auth") {
        post {
          entity(as[LoginCredentials]) { credentials =>
            onSuccess(authenticationService.login(credentials)) {
              case Validated.Valid(Some(jwt)) => completeAsApiResponse(jwt)
              case Validated.Valid(None)      => complete(Forbidden, None)
              case Validated.Invalid(i)       => complete(Unauthorized, Errors(i))
            }
          }
        }
      },
      path("users.context") {
        get {
          authenticate(implicit user => completeAsApiResponse(user))
        }
      },
      path("users.create") {
        post {
          parameters("user_id".as[UUID]) { id =>
            entity(as[UserCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(userService.create(id, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
      path("users.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(userService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("users.get") {
        get {
          parameter("user_id".as[UUID]) { userId =>
            expandParameters(
              "locations",
              "merchant",
              "merchant_setup_steps",
              "access",
              "merchant_legal_details",
            )(
              UserExpansions.withoutPermissions,
            ) { expansions =>
              authenticate { implicit user =>
                onSuccess(userService.findById(userId)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      },
      path("users.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "location_id".as[UUID].?,
              "user_role_id".as[UUID].?,
              "q".?,
              "updated_since".as[ZonedDateTime].?,
            ) { (locationId, userRoleId, query, updatedSince) =>
              expandParameters(
                "locations",
                "merchant",
                "merchant_setup_steps",
                "access",
                "merchant_legal_details",
              )(
                UserExpansions.withoutPermissions,
              ) { expansions =>
                authenticate { implicit user =>
                  val filters = UserFilters.withAccessibleLocations(locationId, userRoleId, query, updatedSince)
                  onSuccess(userService.findAll(filters)(expansions)) { (users, count) =>
                    completeAsPaginatedApiResponse(users, count)
                  }
                }
              }
            }
          }
        }
      },
      path("users.logout") {
        post {
          authenticate { implicit user =>
            extractToken { token =>
              onSuccess(authenticationService.logout(token))(result => completeAsEmptyResponse(result))
            }
          }
        }
      },
      path("users.me") {
        get {
          expandParameters(
            "locations",
            "merchant",
            "merchant_setup_steps",
            "access",
            "merchant_legal_details",
          )(UserExpansions.withPermissions) { expansions =>
            authenticate { implicit user =>
              onSuccess(userService.findById(user.id)(expansions))(result => completeAsOptApiResponse(result))
            }
          }
        }
      },
      path("users.update") {
        post {
          parameter("user_id".as[UUID]) { id =>
            entity(as[UserUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(userService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      },
      path("users.update_active") {
        post {
          entity(as[Seq[UpdateActiveItem]]) { updateActiveItems =>
            authenticate { implicit user =>
              val updateActiveItemWithoutLoggedUser = updateActiveItems.filterNot(_.itemId == user.id)
              onSuccess(userService.updateActiveItems(updateActiveItemWithoutLoggedUser)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      },
      path("users.start_password_reset") {
        post {
          parameter("email".as[String]) { email =>
            passwordResetService.startPasswordReset(email)
            complete(NoContent)
          }
        }
      },
      path("users.password_reset") {
        post {
          entity(as[PasswordReset]) { payload =>
            onSuccess(passwordResetService.passwordReset(payload)) { result =>
              completeAsValidatedApiResponse(result)
            }
          }
        }
      },
    )
}
