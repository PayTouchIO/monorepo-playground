package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.{ Daos, OAuthAppDao }
import io.paytouch.core.data.model.{ OAuthAppRecord, OAuthCodeRecord }
import io.paytouch.core.errors.{ InvalidOauthClientIds, OauthInvalidClientSecret, OauthInvalidRedirectUri }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class OauthAppValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  type Record = OAuthAppRecord
  type Dao = OAuthAppDao

  protected val dao = daos.oauthAppDao
  private val oauthCodeValidator = new OauthCodeValidator()

  def validateOneByClientId(clientId: UUID): Future[ErrorsOr[Record]] =
    dao.findOneByClientId(clientId).map {
      case Some(app) => Multiple.success(app)
      case _         => Multiple.failure(InvalidOauthClientIds(Seq(clientId)))
    }

  def validateAuthorizeRequest(clientId: UUID, redirectUrl: String): Future[ErrorsOr[Record]] =
    validateOneByClientId(clientId).map {
      case vApp @ Valid(app) if validRedirectUrl(app, redirectUrl) => vApp
      case Valid(_)                                                => Multiple.failure(OauthInvalidRedirectUri())
      case i @ Invalid(_)                                          => i
    }

  def validateVerifyRequest(
      clientId: UUID,
      clientSecret: UUID,
      code: UUID,
    ): Future[ErrorsOr[OAuthCodeRecord]] =
    validateOneByClientId(clientId).flatMap {
      case vApp @ Valid(app) if app.clientSecret == clientSecret =>
        validateAppCode(app, code)
      case Valid(_)       => Future.successful(Multiple.failure(OauthInvalidClientSecret()))
      case i @ Invalid(_) => Future.successful(i)
    }

  private def validRedirectUrl(record: Record, redirectUrl: String): Boolean =
    record.redirectUris.split(",").exists(url => redirectUrl.startsWith(url))

  private def validateAppCode(record: Record, code: UUID): Future[ErrorsOr[OAuthCodeRecord]] =
    oauthCodeValidator.validateOneByAppIdAndCode(record.id, code)
}
