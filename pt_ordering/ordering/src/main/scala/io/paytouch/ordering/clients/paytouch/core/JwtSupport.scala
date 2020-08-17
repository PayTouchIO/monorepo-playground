package io.paytouch.ordering.clients.paytouch.core

import java.time.{ Clock, Instant }
import java.util.UUID

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import io.paytouch.ordering.ServiceConfigurations
import io.paytouch.ordering.entities.{ JsonWebToken, _ }

import scala.util.Try

trait JwtSupport extends JwtTokenKeys {

  def generateAuthHeaderForCore(implicit store: StoreContext): Authorization =
    generateAuthHeaderForCoreMerchant(store.merchantId)

  def generateAuthHeaderForCoreMerchant(merchantId: UUID): Authorization = {
    val jwtToken = generateJwtToken(merchantId)
    Authorization(OAuth2BearerToken(jwtToken))
  }

  private def generateJwtToken(merchantId: UUID): JsonWebToken =
    JsonWebToken(
      Map(
        idKey -> merchantId.toString,
        issKey -> "pt_ordering",
        iatKey -> instantNow.getEpochSecond,
      ),
      ServiceConfigurations.coreJwtOrderingSecret,
    )

  protected def instantNow: Instant = Instant.now(Clock.systemUTC)
}

trait JwtTokenKeys {

  protected val idKey = "uid" // u(ser)id
  protected val issKey = "iss" // iss(uer)
  protected val iatKey = "iat" // i(ssued)at
  protected val audKey = "aud" // aud(ience)
  protected val jtiKey = "jti"
  protected val adminKey = "aid" // a(dmin)id

  def getUserId(token: JsonWebToken): Option[UUID] = token.claims.get(idKey).flatMap(recoveredUUID)
  def getJti(token: JsonWebToken): Option[String] = token.claims.get(jtiKey)
  def getSource(token: JsonWebToken): Option[String] = token.claims.get(audKey)
  def getAdminId(token: JsonWebToken): Option[UUID] = token.claims.get(adminKey).flatMap(recoveredUUID)

  private def recoveredUUID(text: String): Option[UUID] = Try(UUID.fromString(text)).toOption

}
