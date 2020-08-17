package io.paytouch.core.resources.auth0

import java.security._
import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }
import pdi.jwt.JwtUtils

class Auth0AuthFSpec extends Auth0FSpec {
  abstract class Auth0AuthFSpecContext extends Auth0ResourceFSpecContext {
    lazy val token = generateAuth0JwtToken()
    lazy val credentials = Auth0Credentials(token, LoginSource.PtDashboard)
  }

  "POST /v1/auth0.auth" in {
    "authenticate a user if credentials are valid" in new Auth0AuthFSpecContext {
      Factory.user(merchant = merchant, userRole = Some(userRole), auth0UserId = Some(subject)).create
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatusOK()

        val jwtTokenEntity = responseAs[ApiResponse[JsonWebToken]].data
        jwtTokenEntity.value.nonEmpty should beTrue
      }
    }

    "reject user authentication if user is deleted" in new Auth0AuthFSpecContext {
      Factory
        .user(merchant, userRole = Some(userRole), deletedAt = Some(UtcTime.now), auth0UserId = Some(subject))
        .create
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }

    "reject user authentication if user is disabled" in new Auth0AuthFSpecContext {
      Factory
        .user(merchant, userRole = Some(userRole), active = Some(false), auth0UserId = Some(subject))
        .create
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }

    "reject user authentication if user has no user role" in new Auth0AuthFSpecContext {
      Factory.user(merchant, userRole = None, auth0UserId = Some(subject)).create
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user's user role has no access to source dashboard" in new Auth0AuthFSpecContext {
      val role = Factory.userRole(merchant, hasDashboardAccess = Some(false)).create
      Factory
        .user(merchant, userRole = Some(role), auth0UserId = Some(subject))
        .create
      override lazy val credentials = Auth0Credentials(token, LoginSource.PtDashboard)
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user's user role has no access to source register" in new Auth0AuthFSpecContext {
      val role = Factory.userRole(merchant, hasRegisterAccess = Some(false)).create
      Factory
        .user(merchant, userRole = Some(role), auth0UserId = Some(subject))
        .create
      override lazy val credentials = Auth0Credentials(token, LoginSource.PtRegister)
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user's user role has no access to source tickets" in new Auth0AuthFSpecContext {
      val role = Factory.userRole(merchant, hasTicketsAccess = Some(false)).create
      Factory
        .user(merchant, userRole = Some(role), auth0UserId = Some(subject))
        .create
      override lazy val credentials = Auth0Credentials(token, LoginSource.PtTickets)
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject authentication for a token without a matching user" in new Auth0AuthFSpecContext {
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }

    "reject authentication for a token with an unacceptable signature" in new Auth0AuthFSpecContext {
      override lazy val privateKey = {
        val generatorRSA = KeyPairGenerator.getInstance(JwtUtils.RSA)
        generatorRSA.initialize(1024)
        generatorRSA.generateKeyPair.getPrivate
      }
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }

    "reject authentication with an invalid token" in new Auth0AuthFSpecContext {
      override lazy val token = "not-a-jwt-token"
      Post("/v1/auth0.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }
  }
}
