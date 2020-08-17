package io.paytouch.core.clients.auth0

import io.paytouch.core.entities.enums.ExposedName.Jwt

sealed trait JwtVerificationError

object JwtVerificationError {
  case object MissingIssuer extends JwtVerificationError {}
  case object MissingKeyId extends JwtVerificationError {}
  case object MissingSubject extends JwtVerificationError {}

  sealed trait WithValue[T] extends JwtVerificationError {
    def value: T
  }

  case class UnexpectedIssuer(value: String) extends WithValue[String] {}
  case class UnexpectedAudience(value: String) extends WithValue[String] {}
  case class UnexpectedAlgorithm(value: String) extends WithValue[String] {}

  case class InvalidToken(value: Throwable) extends WithValue[Throwable] {}
  case class InvalidSignature(value: Throwable) extends WithValue[Throwable] {}

  case class JwkError(value: Throwable) extends WithValue[Throwable] {}
}

final case class Auth0ClientError(
    error: String = "Unknown",
    errorDescription: String = "Unknown",
    // Internal exception in case of parsing failure
    ex: Option[Throwable] = None,
  )
