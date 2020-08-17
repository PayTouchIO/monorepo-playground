package io.paytouch.core.logging

import akka.event.Logging.MDC
import io.paytouch.core.entities.JsonWebToken
import io.paytouch.core.utils.JwtTokenKeys
import io.paytouch.logging._

class MdcActor extends BaseMdcActor with JwtTokenKeys {
  implicit val executionContext = context.dispatcher

  override protected def requestMap(loggedRequest: LoggedRequest): MDC =
    super.requestMap(loggedRequest) ++ jwtTokenMap(loggedRequest)

  private def jwtTokenMap(loggedRequest: LoggedRequest): MDC = {
    val jwtToken: Option[JsonWebToken] =
      loggedRequest.headers.find(_.name == "Authorization").flatMap { header =>
        val Bearer = "Bearer (.*)".r
        header.value match {
          case Bearer(id) => Some(JsonWebToken(id))
          case _          => None
        }
      }
    Map(
      "user_id" -> jwtToken.flatMap(getUserId).getOrElse(`N/A`),
      "admin_id" -> jwtToken.flatMap(getAdminId).getOrElse(`N/A`),
      "jti" -> jwtToken.flatMap(getJti).getOrElse(`N/A`),
      "iss" -> jwtToken.flatMap(getContextSource).map(_.entryName).getOrElse(`N/A`),
      "aud" -> jwtToken.flatMap(getLoginSource).map(_.entryName).getOrElse(`N/A`),
    )
  }

}
