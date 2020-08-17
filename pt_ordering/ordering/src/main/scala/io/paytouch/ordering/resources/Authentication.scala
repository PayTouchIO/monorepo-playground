package io.paytouch.ordering.resources

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.{ AuthenticationDirective, Credentials }
import io.paytouch.ordering.entities.UserContext
import io.paytouch.ordering.services.AuthenticationService
import io.paytouch.ordering.{ ServiceConfigurations => Config }

import scala.concurrent.Future

trait Authentication extends Directives {

  def authenticationService: AuthenticationService

  def userAuthenticate: AuthenticationDirective[UserContext] =
    authenticateOAuth2Async("Paytouch Ordering API", jwtAuthenticator)

  private def jwtAuthenticator(credentials: Credentials): Future[Option[UserContext]] =
    credentials match {
      case Credentials.Provided(id) =>
        val authToken = Authorization(OAuth2BearerToken(id))
        authenticationService.getUserContext(authToken)
      case _ => Future.successful(None)
    }

  def appAuthenticate: AuthenticationDirective[String] =
    authenticateBasic("Paytouch Ordering API", appAuthenticator)

  private def appAuthenticator(credentials: Credentials): Option[String] = {
    import Config._
    credentials match {
      case p @ Credentials.Provided(`storeUser`) if p.verify(storePassword) => Some(storeUser)
      case p @ Credentials.Provided(`coreUser`) if p.verify(corePassword)   => Some(coreUser)
      case _                                                                => None
    }
  }
}
