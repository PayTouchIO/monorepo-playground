package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ OauthCode, UserContext }
import io.paytouch.core.utils.ResultType

trait OauthConversions {

  def toEntityUpsertionResult(result: (ResultType, OAuthCodeRecord)): (ResultType, OauthCode) =
    (result._1, OauthCode(result._2.code))

  def prepareOauthCodeUpdate(oauthAppRecord: OAuthAppRecord)(implicit user: UserContext) =
    OAuthCodeUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      oauthAppId = Some(oauthAppRecord.id),
      code = Some(UUID.randomUUID),
    )

  protected def toAppSessionUpdate(oauthCode: OAuthCodeRecord, session: SessionRecord) =
    OAuthAppSessionUpdate(
      id = None,
      merchantId = Some(oauthCode.merchantId),
      oauthAppId = Some(oauthCode.oauthAppId),
      sessionId = Some(session.id),
    )
}
