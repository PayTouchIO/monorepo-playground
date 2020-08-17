package io.paytouch.core.entities

import io.paytouch.core.entities.enums.ExposedName

final case class LoginResponse(value: String, hashedPin: Option[String]) extends ExposedEntity {

  val classShortName = ExposedName.LoginResponse

  lazy val valueToJwt = JsonWebToken(value)
}
