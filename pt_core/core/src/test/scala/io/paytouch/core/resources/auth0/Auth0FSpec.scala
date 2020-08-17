package io.paytouch.core.resources.auth0

import cats.implicits._
import akka.http.scaladsl.model.headers._
import java.security._
import io.paytouch.core.utils._
import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.utils.Generators

import pdi.jwt._
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm

trait Auth0Fixtures extends Generators {
  lazy val config = Config.auth0Config
  lazy val now: Long = UtcTime.now.toEpochSecond

  lazy val subject = s"google-oauth2|${randomNumericString}"
  lazy val privateKey: PrivateKey = MockedRestApi.jwkClient.getPrivateKey

  def generateAuth0JwtToken(): String = {
    val claim = JwtClaim(
      issuer = config.allowedIssuers.head.value.some,
      subject = subject.some,
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

    val keyId = "key1234"
    val header = JwtHeader(config.algorithm).withKeyId(keyId)
    JwtJson4s.encode(header, claim, privateKey)
  }

  def authHeader(token: String) = Authorization(OAuth2BearerToken(token))
}

abstract class Auth0FSpec extends FSpec {
  abstract class Auth0ResourceFSpecContext extends FSpecContext with DefaultFixtures with Auth0Fixtures {}
}
