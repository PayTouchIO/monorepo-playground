package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import com.github.t3hnar.bcrypt._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.UserRoleRecord
import io.paytouch.core.entities.{ Auth0Credentials, LoginCredentials, UserContext, UserLogin }
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.errors._
import io.paytouch.core.services.{ Auth0Service, UserService }
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr

class AuthenticationValidator(
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) {
  val userRoleDao = daos.userRoleDao

  def validateLoginCredentials(credentials: LoginCredentials): Future[Option[UserLogin]] =
    userService.findUserLoginByEmail(credentials.email).map {
      case Some(userLogin) if credentials.password.isBcrypted(userLogin.encryptedPassword) => Some(userLogin)

      case _ => None
    }

  def validateUserLoginAndSource(userLogin: UserLogin, source: LoginSource): Future[ErrorsOr[UserLogin]] =
    userLogin match {
      case user if user.deletedAt.isDefined => Future.successful(Multiple.failure(UserDeleted()))
      case user if !user.active             => Future.successful(Multiple.failure(UserDisabled()))
      case user if user.isOwner             => Future.successful(Multiple.success(user))
      case user                             => validateSourceAndUserRole(source, user).mapNested(_ => user)
    }

  private def validateSourceAndUserRole(source: LoginSource, userLogin: UserLogin): Future[ErrorsOr[Unit]] =
    userLogin.userRoleId match {
      case None             => Future.successful(Multiple.failure(NoUserRoleAssociated()))
      case Some(userRoleId) => validateSourceAndUserRole(source, userRoleId)
    }

  private def validateSourceAndUserRole(source: LoginSource, userRoleId: UUID): Future[ErrorsOr[Unit]] =
    userRoleDao.findById(userRoleId).map {
      case Some(userRole) if hasAccessToSource(userRole, source) => Multiple.success(())
      case Some(_)                                               => Multiple.failure(InaccessibleSource(source))
      case None                                                  => Multiple.failure(UserRoleNotFound(userRoleId))
    }

  private def hasAccessToSource(userRole: UserRoleRecord, source: LoginSource): Boolean = {
    import LoginSource._

    source match {
      case PtDashboard => userRole.hasDashboardAccess
      case PtRegister  => userRole.hasRegisterAccess
      case PtTickets   => userRole.hasTicketsAccess
      case PtAdmin     => true
    }
  }

  def validateDeletion(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[UUID]]] =
    Future.successful(Multiple.success(ids))
}
