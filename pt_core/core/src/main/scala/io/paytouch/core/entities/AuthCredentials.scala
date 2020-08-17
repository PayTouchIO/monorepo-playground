package io.paytouch.core.entities

import io.paytouch.core.entities.enums.LoginSource

trait AuthCredentials {
  val source: LoginSource
}

final case class LoginCredentials(
    email: String,
    password: String,
    source: LoginSource,
  ) extends AuthCredentials

final case class Auth0Credentials(
    token: String,
    source: LoginSource,
  ) extends AuthCredentials
