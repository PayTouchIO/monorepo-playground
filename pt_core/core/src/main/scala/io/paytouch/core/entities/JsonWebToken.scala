package io.paytouch.core.entities

import java.util.UUID

import authentikat.jwt.{ JsonWebToken => AuthentikatJsonWebToken, _ }

import io.paytouch.core.entities.enums.{ ExposedName, LoginSource }

final case class JsonWebToken(value: String) extends ExposedEntity {
  val classShortName = ExposedName.Jwt

  def isValid(jwtSecret: String): Boolean =
    AuthentikatJsonWebToken.validate(value, jwtSecret)

  val claims: Map[String, String] = value match {
    case AuthentikatJsonWebToken(_, claimsSet, _) =>
      claimsSet.asSimpleMap.getOrElse(Map.empty)

    case _ =>
      Map.empty
  }

  lazy val userId = claims.get("uid").map(UUID.fromString)
  lazy val source = claims.get("aud").map(LoginSource.withNameInsensitive)
  lazy val jti = claims.get("jti")
  lazy val adminId = claims.get("aid")
}

object JsonWebToken {
  implicit def tokenToString(jsonWebToken: JsonWebToken): String = jsonWebToken.value

  def apply(payload: Map[String, Any], jwtSecret: String): JsonWebToken = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(payload)
    val jwt = AuthentikatJsonWebToken(header, claimsSet, jwtSecret)
    JsonWebToken(jwt)
  }
}
