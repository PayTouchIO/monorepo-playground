package io.paytouch.core.resources

import scala.concurrent._

import cats.implicits._

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives._

import io.paytouch.core.entities.{ JsonWebToken, UserContext }
import io.paytouch.core.ServiceConfigurations._
import io.paytouch.core.services.AuthenticationService

trait Authentication extends Directives {
  import Authentication._

  implicit def ec: ExecutionContext

  def authenticationService: AuthenticationService

  final protected lazy val authenticate: AuthenticationDirective[UserContext] =
    authenticateOAuth2Async(Paytouch.Realm, jwtAuthenticator)
      .flatMap(extractPusherSocketIdAndAddToContext)

  private lazy val jwtAuthenticator: Credentials => Future[Option[UserContext]] = {
    case Credentials.Provided(id) if JsonWebToken(id) isValid JwtSecret =>
      authenticationService getUserContext JsonWebToken(id)

    case _ =>
      none.pure[Future]
  }

  private def extractPusherSocketIdAndAddToContext(userContext: UserContext): Directive[Tuple1[UserContext]] =
    optionalHeaderValue(extractPusherSocketIdHeader).map { pusherSocketId =>
      userContext.copy(pusherSocketId = pusherSocketId)
    }

  private lazy val extractPusherSocketIdHeader: HttpHeader => Option[String] = {
    case HttpHeader(Paytouch.PusherSocketIdHeader, value) => value.some
    case _                                                => none
  }

  final protected lazy val userOrAppAuthenticate: AuthenticationDirective[UserContext] =
    authenticateOAuth2Async(Paytouch.Realm, jwtUserOrAppAuthenticator)
      .flatMap(extractPusherSocketIdAndAddToContext)

  private def jwtUserOrAppAuthenticator(credentials: Credentials): Future[Option[UserContext]] =
    jwtAuthenticator(credentials).flatMap {
      case None        => appAuthenticator(credentials)
      case userContext => userContext.pure[Future]
    }

  final protected lazy val appAuthenticate: AuthenticationDirective[UserContext] =
    authenticateOAuth2Async(Paytouch.Realm, appAuthenticator)
      .flatMap(extractPusherSocketIdAndAddToContext)

  private lazy val appAuthenticator: Credentials => Future[Option[UserContext]] = {
    case Credentials.Provided(id) if JsonWebToken(id) isValid JwtOrderingSecret =>
      authenticationService.getOwnerUserContextForApp(JsonWebToken(id))

    case _ =>
      none.pure[Future]
  }

  final protected lazy val extractToken: AuthenticationDirective[JsonWebToken] =
    authenticateOAuth2(Paytouch.TokenExtractorRealm, jwtExtractor)

  private lazy val jwtExtractor: Credentials => Option[JsonWebToken] = {
    case Credentials.Provided(id) => JsonWebToken(id).some
    case _                        => none
  }
}

object Authentication {
  private object Paytouch {
    val Realm: String =
      "Paytouch Core API"

    val TokenExtractorRealm: String =
      s"$Realm - token extractor"

    val PusherSocketIdHeader: String =
      "paytouch-pusher-socket-id"
  }
}
