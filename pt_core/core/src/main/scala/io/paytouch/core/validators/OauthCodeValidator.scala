package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, OAuthCodeDao }
import io.paytouch.core.data.model.OAuthCodeRecord
import io.paytouch.core.errors.OauthInvalidCode
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class OauthCodeValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  type Record = OAuthCodeRecord
  type Dao = OAuthCodeDao

  protected val dao = daos.oauthCodeDao

  def validateOneByAppIdAndCode(oauthAppId: UUID, code: UUID): Future[ErrorsOr[Record]] =
    dao.findOneByAppIdAndCode(oauthAppId, code).map {
      case Some(oauthCode) => Multiple.success(oauthCode)
      case _               => Multiple.failure(OauthInvalidCode())
    }

}
