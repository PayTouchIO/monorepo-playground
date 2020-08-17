package io.paytouch.core.services

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import io.paytouch.core.conversions.OauthConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OAuthCodeRecord, SessionRecord }
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.entities.{ LoginResponse, OauthCode, UserContext }
import io.paytouch.core.errors.InvalidUserIds
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.OauthAppValidator

import scala.concurrent._

class OauthService(authenticationService: AuthenticationService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends OauthConversions {

  private val validator = new OauthAppValidator()

  def authorize(
      clientId: UUID,
      redirectUri: String,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[OauthCode]]] =
    validator.validateAuthorizeRequest(clientId, redirectUri).flatMapTraverse { oauthApp =>
      daos.oauthCodeDao.upsert(prepareOauthCodeUpdate(oauthApp)).map(toEntityUpsertionResult)
    }

  def verify(
      clientId: UUID,
      clientSecret: UUID,
      code: UUID,
    ): Future[ErrorsOr[LoginResponse]] =
    validator.validateVerifyRequest(clientId, clientSecret, code).flatMap {
      case Valid(oauthCode) =>
        authenticationService
          .createSessionForUser(oauthCode.userId, LoginSource.PtDashboard)
          .flatMap(createAppSessionPairOrFail(oauthCode, _))
      case i @ Invalid(_) => Future.successful(i)
    }

  private def createAppSessionPairOrFail(oauthCode: OAuthCodeRecord, result: Option[(LoginResponse, SessionRecord)]) =
    result match {
      case Some((loginResponse, session)) => createAppSessionPair(oauthCode, loginResponse, session)
      case _                              => Future.successful(Multiple.failure(InvalidUserIds(Seq(oauthCode.userId))))
    }

  private def createAppSessionPair(
      oauthCode: OAuthCodeRecord,
      loginResponse: LoginResponse,
      session: SessionRecord,
    ) =
    daos
      .oauthAppSessionDao
      .upsert(toAppSessionUpdate(oauthCode, session))
      .map(_ => Multiple.success(loginResponse))
}
