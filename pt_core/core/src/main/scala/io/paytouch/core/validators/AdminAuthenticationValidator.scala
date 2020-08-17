package io.paytouch.core.validators

import cats.data.Validated.{ Invalid, Valid }
import com.github.t3hnar.bcrypt._
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ AdminLogin, AdminLoginCredentials }
import io.paytouch.core.errors.UnsupportedHostedDomain
import io.paytouch.core.services.AdminService
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils._

import scala.concurrent._

class AdminAuthenticationValidator(val adminService: AdminService)(implicit val ec: ExecutionContext, val daos: Daos) {

  def validateCredentialsAuthentication(credentials: AdminLoginCredentials): Future[ErrorsOr[Option[AdminLogin]]] =
    validateUserExistenceWithStrategy(
      credentials.email,
      admin => credentials.password.isBcrypted(admin.encryptedPassword),
    )

  def validateUserExistence(email: String): Future[ErrorsOr[Option[AdminLogin]]] =
    validateUserExistenceWithStrategy(email, admin => true)

  private def validateUserExistenceWithStrategy(
      email: String,
      extraUserCheck: AdminLogin => Boolean,
    ): Future[ErrorsOr[Option[AdminLogin]]] =
    adminService.findAdminInfoByEmail(email).map {
      case Some(admin) if extraUserCheck(admin) => Multiple.successOpt(admin)
      case _                                    => Multiple.empty
    }

  def validateGooglePayload(payload: GoogleIdToken.Payload): Future[ErrorsOr[Option[AdminLogin]]] =
    validateGoogleHostedDomain(payload) match {
      case Valid(a)       => validateUserExistence(payload.getEmail)
      case i @ Invalid(_) => Future.successful(i)
    }

  private def validateGoogleHostedDomain(payload: GoogleIdToken.Payload): ErrorsOr[Boolean] = {
    val validDomains = Seq("paytouch.io", "paytouch.com")
    if (validDomains.contains(payload.getHostedDomain)) Multiple.success(true)
    else Multiple.failure(UnsupportedHostedDomain())
  }
}
