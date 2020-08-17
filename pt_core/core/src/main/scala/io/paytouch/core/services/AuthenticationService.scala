package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.data._
import cats.implicits._

import io.paytouch.core.async.monitors.{ AuthenticationMonitor, SuccessfulLogin }
import io.paytouch.core.data.daos.{ Daos, SessionDao }
import io.paytouch.core.data.model.SessionRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ ContextSource, LoginSource }
import io.paytouch.core.errors.NonAccessibleUserIds
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.SessionFilters
import io.paytouch.core.JwtSecret
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.AuthenticationValidator
import io.paytouch.utils.Tagging.withTag

class AuthenticationService(
    val jwtSecret: String withTag JwtSecret,
    val monitor: ActorRef withTag AuthenticationMonitor,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends JwtTokenGenerator
       with FindAllFeature {
  type Dao = SessionDao
  type Entity = Session
  type Expansions = NoExpansions
  type Record = SessionRecord
  type Filters = SessionFilters
  type Validator = AuthenticationValidator

  protected val validator = new AuthenticationValidator(userService)
  val sessionDao = daos.sessionDao
  val dao = sessionDao

  def login(credentials: LoginCredentials): Future[ErrorsOr[Option[LoginResponse]]] =
    validator.validateLoginCredentials(credentials: LoginCredentials).flatMap {
      case Some(userLogin) =>
        validateUserLoginAndSource(userLogin, credentials)

      case None =>
        Multiple.empty.pure[Future]
    }

  def validateUserLoginAndSource(
      userLogin: UserLogin,
      credentials: AuthCredentials,
    ): Future[ErrorsOr[Option[LoginResponse]]] =
    validateUserLoginAndSource(userLogin, credentials.source)
      .map(_.map(_.some))

  def validateUserLoginAndSource(
      userLogin: UserLogin,
      loginSource: LoginSource,
    ): Future[ErrorsOr[LoginResponse]] =
    validator.validateUserLoginAndSource(userLogin, loginSource).flatMapTraverse { _ =>
      monitor ! SuccessfulLogin(userLogin, loginSource, UtcTime.ofInstant(thisInstant))

      createSession(userLogin, loginSource)
        .map(session => toLoginResponse(userLogin, session))
    }

  def createSessionForOwner(merchantId: UUID): Future[Option[LoginResponse]] =
    (for {
      owner <- OptionT(userService.findOwner(merchantId))
      ownerUserLogin = owner.toUserLogin
      session <- OptionT.liftF(createSession(ownerUserLogin, LoginSource.PtDashboard))
    } yield toLoginResponse(ownerUserLogin, session)).value

  def createSessionForUser(userId: UUID, loginSource: LoginSource): Future[Option[(LoginResponse, SessionRecord)]] =
    (for {
      user <- OptionT(userService.findActiveUserLoginById(userId))
      session <- OptionT.liftF(createSession(user, loginSource))
    } yield (toLoginResponse(user, session), session)).value

  def createValidatedSessionForUser(
      userId: UUID,
      loginSource: LoginSource,
    ): Future[ErrorsOr[LoginResponse]] =
    (for {
      user <- OptionT(userService.findUserLoginById(userId))
      response <- OptionT.liftF(validateUserLoginAndSource(user, loginSource))
    } yield response).value.map(_.getOrElse(NonAccessibleUserIds(Seq(userId)).invalidNel))

  def getUserContext(jwtToken: JsonWebToken): Future[Option[UserContext]] =
    sessionDao.access(jwtToken).flatMap {
      case Some(session) => userService.getUserContext(session.userId, session.source.toContextSource)
      case None          => Future.successful(None)
    }

  def getOwnerUserContextForApp(jwtToken: JsonWebToken): Future[Option[UserContext]] =
    (for {
      merchantId <- OptionT.fromOption[Future](getUserId(jwtToken))
      source <- OptionT.fromOption[Future](getContextSourceWithWarning(jwtToken))
      owner <- OptionT(userService.findOwner(merchantId))
      userContext <- OptionT(userService.getUserContext(owner.id, source))
    } yield userContext).value

  def logout(token: JsonWebToken): Future[ErrorsOr[Unit]] =
    sessionDao
      .delete(token)
      .map(Multiple.success)

  def deleteSessionsByMerchantId(merchantId: UUID): Future[Boolean] =
    sessionDao
      .deleteByMerchantId(merchantId)
      .map(_ > 0)

  def deleteSessionsByUserId(merchantId: UUID): Future[Boolean] =
    sessionDao
      .deleteByUserId(merchantId)
      .map(_ > 0)

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    records
      .map(r => Session(r.id, r.source, r.createdAt, r.updatedAt))
      .pure[Future]

  def bulkDelete(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    validator
      .validateDeletion(ids)
      .flatMapTraverse(validatedBulkDelete)

  protected def getContextSourceWithWarning(jwtToken: JsonWebToken): Option[ContextSource] =
    getContextSource(jwtToken).orElse {
      logger.warn(s"Could not map iss (${getIss(jwtToken)}) to context source.")
      None
    }

  protected def validatedBulkDelete(ids: Seq[UUID])(implicit user: UserContext) =
    dao
      .deleteByIdsAndUserId(ids, user.id)
      .void
}
