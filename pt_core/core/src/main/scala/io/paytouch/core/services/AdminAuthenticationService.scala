package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import cats.data.Validated.{ Invalid, Valid }
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import io.paytouch.core.async.monitors.{ AdminAuthenticationMonitor, SuccessfulAdminLogin }
import io.paytouch.core.conversions.AdminAuthenticationConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.errors.{ AdminPasswordAuthDisabledError, GoogleIdTokenValidationError, InvalidUserIds }
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils.{ JwtTokenGenerator, Multiple, UtcTime }
import io.paytouch.core.validators.AdminAuthenticationValidator
import io.paytouch.core.{ withTag, AdminPasswordAuthEnabled, JwtSecret }

import scala.concurrent._

class AdminAuthenticationService(
    val jwtSecret: String withTag JwtSecret,
    val adminPasswordAuthEnabled: Boolean withTag AdminPasswordAuthEnabled,
    val monitor: ActorRef withTag AdminAuthenticationMonitor,
    val adminService: AdminService,
    val googleAuthenticationService: GoogleAuthenticationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends JwtTokenGenerator
       with AdminAuthenticationConversions {

  protected val validator = new AdminAuthenticationValidator(adminService)
  val adminDao = daos.adminDao
  val sessionDao = daos.sessionDao

  def login(credentials: AdminLoginCredentials): Future[ErrorsOr[Option[LoginResponse]]] =
    if (adminPasswordAuthEnabled)
      validator.validateCredentialsAuthentication(credentials).map {
        case Valid(Some(admin)) => loginAdmin(admin)
        case Valid(None)        => Multiple.empty
        case i @ Invalid(_)     => i
      }
    else
      Future.successful(Multiple.failure(AdminPasswordAuthDisabledError()))

  def checkGoogleTokenAndLogin(idTokenString: String): Future[ErrorsOr[Option[LoginResponse]]] = {
    def createAdminLoginFromPayload(payload: GoogleIdToken.Payload): Future[AdminLogin] =
      adminDao.upsert(fromGooglePayloadToUpdate(payload)).map {
        case (_, adminRecord) => AdminLogin.fromRecord(adminRecord)
      }

    googleAuthenticationService.verify(idTokenString).flatMap {
      case Right(Some(payload)) =>
        validator.validateGooglePayload(payload).flatMap {
          case Valid(Some(admin)) => Future.successful(loginAdmin(admin))
          case Valid(None)        => createAdminLoginFromPayload(payload).map(loginAdmin)
          case i @ Invalid(_)     => Future.successful(i)
        }
      case Right(None)     => Future.successful(Multiple.failure(GoogleIdTokenValidationError()))
      case Left(exception) => Future.failed(exception)
    }
  }

  private def loginAdmin(admin: AdminLogin) = {
    monitor ! SuccessfulAdminLogin(admin, UtcTime.ofInstant(thisInstant))
    Multiple.successOpt(generateAdminLoginResponse(admin.id))
  }

  def getAdminContext(jwtToken: JsonWebToken): Future[Option[AdminContext]] =
    jwtToken.claims.get(idKey) match {
      case Some(id) => adminService.getAdminContext(UUID.fromString(id))
      case None     => Future.successful(None)
    }

  def loginAs(userId: UUID, source: LoginSource)(implicit admin: AdminContext): Future[ErrorsOr[LoginResponse]] =
    daos.userDao.findActiveUserLoginById(userId).flatMap {
      case Some(user) =>
        createSession(user, source, Some(admin.id))
          .map(session => Multiple.success(toLoginResponse(user, session)))
      case None => Future.successful(Multiple.failure(InvalidUserIds(Seq(userId))))
    }
}
