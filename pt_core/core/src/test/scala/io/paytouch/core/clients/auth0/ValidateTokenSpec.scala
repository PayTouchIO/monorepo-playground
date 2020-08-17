package io.paytouch.core.clients.auth0

import cats.data._
import cats.implicits._

import java.security.KeyPairGenerator
import JwtVerificationError._

import io.paytouch.core.stubs.JwkStubClient
import io.paytouch.core.utils._
import io.paytouch.implicits._

import org.specs2.specification.Scope

import pdi.jwt._
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm

import scala.concurrent._

class ValidateTokenSpec extends PaytouchSpec {
  abstract class ValidateTokenSpecContext extends Scope {
    implicit val system = MockedRestApi.testAsyncSystem
    implicit val mdcActor = MockedRestApi.mdcActor

    lazy val config = Auth0Config(
      algorithm = JwtAlgorithm.RS256,
      apiIdentifier = "https://core.paytouch.io".pipe(Auth0Config.ApiIdentifier),
      allowedIssuers = Seq("https://issuer.paytouch.auth0.com/".pipe(Auth0Config.AllowedIssuer)),
    )

    def generateRsaKey = {
      val generatorRSA = KeyPairGenerator.getInstance(JwtUtils.RSA)
      generatorRSA.initialize(1024)
      generatorRSA.generateKeyPair
    }

    lazy val baseRsaKey = generateRsaKey

    lazy val rsaKey = baseRsaKey

    lazy val jwkClient = new JwkStubClient
    jwkClient.recordKeyPair(baseRsaKey)

    lazy val client = new Auth0Client(config, jwkClient)

    lazy val now: Long = UtcTime.now.toEpochSecond

    lazy val baseClaim: JwtClaim = JwtClaim(
      issuer = config.allowedIssuers.head.value.some,
      subject = "google-oauth2|102298750279617101317".some,
      audience = Set(config.apiIdentifier.value).some,
      content = JwtUtils.hashToJson(
        Seq(
          ("azp", "XJgK0W6mDpAOgAOMrcL4nKR09qrG3oh7"),
          ("scope", "openid profile email"),
        ),
      ),
      issuedAt = (now - 123).some,
      expiration = (now + 300).some,
    )

    lazy val jwtClaim = baseClaim
    lazy val keyId = "key1234"

    lazy val jwtAlgorithm: JwtAsymmetricAlgorithm = config.algorithm
    lazy val jwtToken: String = {
      val header = JwtHeader(jwtAlgorithm).withKeyId(keyId)
      JwtJson4s.encode(header, jwtClaim, rsaKey.getPrivate)
    }
  }

  "Auth0Client" should {
    "#validateJwtToken" should {
      "if the token is valid" should {
        "return success" in new ValidateTokenSpecContext {
          val result = client.validateJwtToken(jwtToken).await
          result must beRight
        }
      }

      "if the token has an unexpected algorithm" should {
        "return an error" in new ValidateTokenSpecContext {
          override lazy val jwtAlgorithm = JwtAlgorithm.RS512
          val result = client.validateJwtToken(jwtToken).await
          result must beLeft.like { case InvalidSignature(_) => ok }
        }
      }

      "if the token has an unexpected issuer" should {
        "return an error" in new ValidateTokenSpecContext {
          override lazy val jwtClaim = baseClaim.by("https://invalid-issuer.auth0.com")
          val result = client.validateJwtToken(jwtToken).await
          result must beLeft(UnexpectedIssuer("https://invalid-issuer.auth0.com"))
        }
      }

      "if the token has multiple audiences" should {
        "return success" in new ValidateTokenSpecContext {
          override lazy val jwtClaim = baseClaim.to(
            Set(
              "https://dev-hjc6ee9j.us.auth0.com/userinfo",
              config.apiIdentifier.value,
            ),
          )
          val result = client.validateJwtToken(jwtToken).await
          result must beRight
        }
      }

      "if the token has an unexpected audience" should {
        "return an error" in new ValidateTokenSpecContext {
          override lazy val jwtClaim = baseClaim.to(Set("https://invalid-audience.paytouch.com"))
          val result = client.validateJwtToken(jwtToken).await
          result must beLeft(UnexpectedAudience("https://invalid-audience.paytouch.com"))
        }
      }

      "if the token has an invalid signature" should {
        "return an error" in new ValidateTokenSpecContext {
          // Signed by a different key
          override lazy val rsaKey = generateRsaKey
          val result = client.validateJwtToken(jwtToken).await
          result must beLeft.like { case InvalidSignature(_) => ok }
        }
      }

      "if the token is expired" should {
        "return an error" in new ValidateTokenSpecContext {
          override lazy val jwtClaim = baseClaim.expiresAt(now - 30)
          val result = client.validateJwtToken(jwtToken).await
          result must beLeft.like { case InvalidToken(_) => ok }
        }
      }

      "if the token has the wrong format" should {
        "return an error" in new ValidateTokenSpecContext {
          val validJwtToken = "invalid-format-token"

          val result = client.validateJwtToken(validJwtToken).await
          result must beLeft.like { case InvalidToken(_) => ok }
        }
      }
    }
  }
}
