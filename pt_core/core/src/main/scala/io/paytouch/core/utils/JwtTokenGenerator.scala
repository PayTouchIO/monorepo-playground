package io.paytouch.core.utils

import java.util.UUID

import scala.concurrent._
import scala.util.Try

import io.paytouch.core.data.daos.SessionDao
import io.paytouch.core.data.model.SessionRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.json.serializers.EnumKeySerializer

trait JwtTokenGenerator extends JwtTokenKeys {
  val jwtSecret: String
  val sessionDao: SessionDao
  implicit def ec: ExecutionContext

  protected def createSession(
      user: UserLogin,
      source: LoginSource,
      adminId: Option[UUID] = None,
    ): Future[SessionRecord] =
    sessionDao.create(
      userId = user.id,
      source = source,
      adminId = adminId,
    )

  protected def toLoginResponse(user: UserLogin, session: SessionRecord): LoginResponse = {
    val token = generateUserJsonWebToken(session)

    LoginResponse(token.value, user.hashedPin)
  }

  protected def generateUserJsonWebToken(session: SessionRecord) =
    JsonWebToken(
      Map(
        idKey -> session.userId.toString,
        issKey -> "ptCore",
        iatKey -> session.createdAt.toInstant.getEpochSecond,
        audKey -> session.source.entryName,
        jtiKey -> session.jti,
        adminKey -> session.adminId.map(_.toString).getOrElse(""),
      ),
      jwtSecret,
    )

  protected def generateAdminLoginResponse(id: UUID) = {
    val token = JsonWebToken(
      Map(
        idKey -> id.toString,
        issKey -> "ptCore",
        iatKey -> thisInstant.getEpochSecond,
        jtiKey -> generateUuid.toString.replace("-", ""),
      ),
      jwtSecret,
    )

    LoginResponse(token.value, None)
  }

  protected def thisInstant = UtcTime.thisInstant

  protected def generateUuid = UUID.randomUUID
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
  def getIss(token: JsonWebToken): Option[String] = token.claims.get(issKey)
  def getAud(token: JsonWebToken): Option[String] = token.claims.get(audKey)
  def getAdmin(token: JsonWebToken): Option[String] = token.claims.get(adminKey)

  def getContextSource(token: JsonWebToken): Option[ContextSource] =
    getIss(token).flatMap(EnumKeySerializer.deserialize(ContextSource).lift)

  def getLoginSource(token: JsonWebToken): Option[LoginSource] =
    getAud(token).flatMap(LoginSource.withNameOption)

  def getAdminId(token: JsonWebToken): Option[UUID] =
    getAdmin(token).flatMap(recoveredUUID)

  private def recoveredUUID(text: String): Option[UUID] =
    Try(UUID.fromString(text)).toOption
}
