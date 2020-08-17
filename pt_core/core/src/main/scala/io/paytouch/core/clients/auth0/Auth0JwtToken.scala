package io.paytouch.core.clients.auth0

import io.paytouch._
import cats.implicits._
import pdi.jwt._

final case class Auth0JwtToken(
    token: String,
    header: JwtHeader,
    claim: JwtClaim,
  ) {
  lazy val issuer: Option[String] = claim.issuer
  lazy val audience: Set[String] = claim.audience.getOrElse(Set.empty)
  lazy val keyId: Option[String] = header.keyId
  lazy val subject: Option[String] = claim.subject
}

final case class ValidAuth0JwtToken(
    token: String,
    header: JwtHeader,
    claim: JwtClaim,
    issuer: String,
    auth0UserId: Auth0UserId,
  ) {}

object ValidAuth0JwtToken {
  def apply(
      jwtToken: Auth0JwtToken,
      issuer: String,
      subject: String,
    ): ValidAuth0JwtToken =
    ValidAuth0JwtToken(
      token = jwtToken.token,
      header = jwtToken.header,
      claim = jwtToken.claim,
      issuer = issuer,
      auth0UserId = Auth0UserId(subject),
    )
}
