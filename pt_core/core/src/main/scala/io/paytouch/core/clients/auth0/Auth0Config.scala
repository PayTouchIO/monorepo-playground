package io.paytouch.core.clients.auth0

import io.paytouch.{ Opaque, OpaqueCompanion }
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import Auth0Config._

final case class Auth0Config(
    algorithm: JwtAsymmetricAlgorithm,
    apiIdentifier: ApiIdentifier,
    allowedIssuers: Seq[AllowedIssuer],
  )

object Auth0Config {
  final case class ApiIdentifier(value: String) extends Opaque[String]
  case object ApiIdentifier extends OpaqueCompanion[String, ApiIdentifier]

  final case class AllowedIssuer(value: String) extends Opaque[String]
  case object AllowedIssuer extends OpaqueCompanion[String, AllowedIssuer]
}
