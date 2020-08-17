package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route

import cats.data._

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.errors.Errors
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.AdminService

trait AdminResource extends JsonResource with AdminAuthentication {
  def adminService: AdminService

  val adminRoutes: Route =
    path("admins.auth") {
      post {
        entity(as[AdminLoginCredentials]) { credentials =>
          onSuccess(adminAuthenticationService.login(credentials)) {
            case Validated.Valid(Some(jwt)) => completeAsApiResponse(jwt)
            case Validated.Valid(None)      => complete(Forbidden, None)
            case Validated.Invalid(i)       => complete(Unauthorized, Errors(i))
          }
        }
      }
    } ~
      path("admins.create") {
        post {
          parameters("admin_id".as[UUID]) { id =>
            entity(as[AdminCreation]) { creation =>
              authenticateAdmin { implicit admin =>
                onSuccess(adminService.create(id, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("admins.get") {
        get {
          parameter("admin_id".as[UUID]) { adminId =>
            authenticateAdmin { implicit admin =>
              onSuccess(adminService.findById(adminId)(NoExpansions()))(result => completeAsOptApiResponse(result))
            }
          }
        }
      } ~
      path("admins.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            authenticateAdmin { implicit admin =>
              onSuccess(adminService.findAll(NoFilters())(NoExpansions())) { (admins, count) =>
                completeAsPaginatedApiResponse(admins, count)
              }
            }
          }
        }
      } ~
      path("admins.login_as") {
        post {
          parameter("user_id".as[UUID], "source".as[LoginSource]) {
            case (userId, source) =>
              authenticateAdmin { implicit admin =>
                onSuccess(adminAuthenticationService.loginAs(userId, source)) {
                  case Validated.Valid(jwt) => completeAsApiResponse(jwt)
                  case Validated.Invalid(i) => complete(NotFound, Errors(i))
                }
              }
          }
        }
      } ~
      path("admins.me") {
        get {
          authenticateAdmin { implicit admin =>
            onSuccess(adminService.findById(admin.id)(NoExpansions()))(result => completeAsOptApiResponse(result))
          }
        }
      } ~
      path("admins.update") {
        post {
          parameter("admin_id".as[UUID]) { id =>
            entity(as[AdminUpdate]) { update =>
              authenticateAdmin { implicit admin =>
                onSuccess(adminService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("admins.google_auth") {
        post {
          entity(as[GoogleIdToken]) { body =>
            onSuccess(adminAuthenticationService.checkGoogleTokenAndLogin(body.idToken)) {
              case Validated.Valid(Some(jwt)) => completeAsApiResponse(jwt)
              case Validated.Valid(None)      => complete(Forbidden, None)
              case Validated.Invalid(i)       => complete(Unauthorized, Errors(i))
            }
          }
        }
      }
}
