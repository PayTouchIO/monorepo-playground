package io.paytouch.core.resources.auth0

import java.security._
import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }
import pdi.jwt.JwtUtils

class Auth0ValidateFSpec extends Auth0FSpec {
  abstract class Auth0ValidateFSpecContext extends Auth0ResourceFSpecContext {}

  "POST /v1/auth0.validate" in {
    "return no content when successful" in new Auth0ValidateFSpecContext {
      Factory.user(merchant = merchant, auth0UserId = Some(subject)).create
      val token = generateAuth0JwtToken()
      Post("/v1/auth0.validate").addHeader(authHeader(token)) ~> routes ~> check {
        assertStatus(StatusCodes.NoContent)
      }
    }

    "return forbidden for a token without a user" in new Auth0ValidateFSpecContext {
      val token = generateAuth0JwtToken()
      Post("/v1/auth0.validate").addHeader(authHeader(token)) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }

    "return unauthorized for a token with an unacceptable signature" in new Auth0ValidateFSpecContext {
      override lazy val privateKey = {
        val generatorRSA = KeyPairGenerator.getInstance(JwtUtils.RSA)
        generatorRSA.initialize(1024)
        generatorRSA.generateKeyPair.getPrivate
      }

      val token = generateAuth0JwtToken()
      Post("/v1/auth0.validate").addHeader(authHeader(token)) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "return unauthorized for an invalid token" in new Auth0ValidateFSpecContext {
      val token = "not-a-jwt-token"
      Post("/v1/auth0.validate").addHeader(authHeader(token)) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "if the token is missing" in new Auth0ValidateFSpecContext {
      Post("/v1/auth0.validate") ~> routes ~> check {
        assertStatus(StatusCodes.BadRequest)
      }
    }
  }
}
