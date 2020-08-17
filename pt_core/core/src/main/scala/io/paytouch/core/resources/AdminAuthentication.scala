package io.paytouch.core.resources

import scala.concurrent._

import cats.implicits._

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives._

import io.paytouch.core.ServiceConfigurations._
import io.paytouch.core.entities.{ AdminContext, JsonWebToken }
import io.paytouch.core.services.AdminAuthenticationService

trait AdminAuthentication extends Directives {
  import AdminAuthentication._

  def adminAuthenticationService: AdminAuthenticationService

  final protected lazy val authenticateAdmin: AuthenticationDirective[AdminContext] =
    authenticateOAuth2Async(Paytouch.Realm, jwtAuthenticator)

  private lazy val jwtAuthenticator: Credentials => Future[Option[AdminContext]] = {
    case Credentials.Provided(id) if JsonWebToken(id) isValid JwtSecret =>
      adminAuthenticationService getAdminContext JsonWebToken(id)

    case _ =>
      Future.successful(none)
  }
}

object AdminAuthentication {
  private object Paytouch {
    val Realm: String =
      "Paytouch Core Admin"
  }
}
