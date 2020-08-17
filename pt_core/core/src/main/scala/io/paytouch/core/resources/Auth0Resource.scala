package io.paytouch.core.resources

import cats.data.Validated._

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives._

import io.paytouch.core.errors.Errors
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.services.{ Auth0Service, AuthenticationService }
import io.paytouch.utils.RejectionMsg

trait Auth0Resource extends JsonResource {

  def auth0Service: Auth0Service
  def authenticationService: AuthenticationService

  val auth0Routes: Route =
    path("auth0.validate") {
      post {
        extractOAuth2Token { token =>
          onSuccess(auth0Service.validateToken(token)) {
            case Valid(Some(_)) => complete(NoContent, None)
            case Valid(None)    => complete(Forbidden, None)
            case Invalid(i)     => complete(Unauthorized, Errors(i))
          }
        }
      }
    } ~
      path("auth0.auth") {
        post {
          entity(as[Auth0Credentials]) { credentials =>
            onSuccess(auth0Service.login(credentials)) {
              case Valid(Some(jwt)) => completeAsApiResponse(jwt)
              case Valid(None)      => complete(Forbidden, None)
              case Invalid(i)       => complete(Unauthorized, Errors(i))
            }
          }
        }
      } ~
      path("auth0.registration") {
        post {
          entity(as[Auth0Registration]) { creation =>
            onSuccess(auth0Service.registration(creation))(result => completeAsApiResponse(result))
          }
        }
      }

  private def extractOAuth2Token: Directive1[String] =
    extractCredentials.flatMap {
      case Some(c: OAuth2BearerToken) => provide(c.token)
      case _                          => complete(BadRequest, RejectionMsg("Missing OAuth2 Bearer token"))
    }

}
