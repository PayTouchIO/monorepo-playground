package io.paytouch.core.clients.auth0

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.{ Base64, Currency }

import akka.http.scaladsl.model.{ HttpMethod, HttpMethods, HttpRequest }
import io.paytouch.core.{ StripeBaseUri, StripeSecretKey }
import io.paytouch.core.clients.ClientSpec
import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.utils.FixturesSupport
import io.paytouch.utils.Tagging._
import org.json4s.JsonAST._

import cats.data._
import cats.implicits._

import java.security.KeyPairGenerator
import JwtVerificationError._

import io.paytouch._
import io.paytouch.core.clients.auth0.entities._
import io.paytouch.core.stubs.JwkStubClient
import io.paytouch.core.utils._
import io.paytouch.implicits._

import org.specs2.specification.Scope

import pdi.jwt._
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm

import scala.concurrent._
import java.time.ZoneId
import akka.http.scaladsl.model.StatusCodes

class UserInfoSpec extends ClientSpec {
  lazy val token = ValidAuth0JwtToken(
    token = "some-jwt-token",
    header = JwtHeader(),
    claim = JwtClaim(),
    issuer = "https://some-auth0-issuer/",
    auth0UserId = Auth0UserId("auth0|2131213"),
  )

  abstract class UserInfoSpecContext extends UserInfoClient(token.issuer) with ClientSpecContext with Fixtures {
    def assertRequest(
        request: HttpRequest,
        method: HttpMethod,
        path: String,
        token: String,
      ) = {
      request.method ==== method
      request.uri.path.toString ==== path
      request.headers.find(h => h.name == "Authorization").get.value ==== s"Bearer ${token}"
    }
  }

  "Auth0Client" should {
    "#userInfo" should {
      "fetch user info from a token" in new UserInfoSpecContext {
        val response =
          when(userInfo(token))
            .expectRequest(request =>
              assertRequest(
                request,
                HttpMethods.GET,
                s"/userinfo",
                token.token,
              ),
            )
            .respondWith("/auth0/responses/userinfo.json")

        response.await ==== Right(userInfo)
      }

      "return an error if the token is invalid" in new UserInfoSpecContext {
        val response =
          when(userInfo(token))
            .expectRequest(request =>
              assertRequest(
                request,
                HttpMethods.GET,
                s"/userinfo",
                token.token,
              ),
            )
            .respondWithError(StatusCodes.Unauthorized)

        response.await ==== Left(Auth0ClientError())
      }
    }
  }

  trait Fixtures {
    lazy val userInfo = UserInfo(
      name = "Jane Josephine Doe",
      email = "janedoe@exampleco.com",
      emailVerified = true,
      zoneinfo = ZoneId.of("America/Los_Angeles").some,
    )
  }
}
