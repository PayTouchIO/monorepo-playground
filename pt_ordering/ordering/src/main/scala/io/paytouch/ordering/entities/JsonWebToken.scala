package io.paytouch.ordering.entities

import authentikat.jwt.{ JsonWebToken => AuthentikatJsonWebToken, _ }

final case class JsonWebToken(value: String) {

  val claims: Map[String, String] = value match {
    case AuthentikatJsonWebToken(_, claimsSet, _) =>
      claimsSet.asSimpleMap.getOrElse(Map.empty)
    case _ => Map.empty
  }
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
