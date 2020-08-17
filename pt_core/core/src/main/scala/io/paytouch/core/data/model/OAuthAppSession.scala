package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OAuthAppSessionRecord(
    id: UUID,
    merchantId: UUID,
    oauthAppId: UUID,
    sessionId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OAuthAppSessionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    oauthAppId: Option[UUID],
    sessionId: Option[UUID],
  ) extends SlickMerchantUpdate[OAuthAppSessionRecord] {

  def toRecord: OAuthAppSessionRecord = {
    require(merchantId.isDefined, s"Impossible to convert OAuthAppSessionUpdate without a merchant id. [$this]")
    require(oauthAppId.isDefined, s"Impossible to convert OAuthAppSessionUpdate without a oauth app id. [$this]")
    require(sessionId.isDefined, s"Impossible to convert OAuthAppSessionUpdate without a session id. [$this]")
    OAuthAppSessionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      oauthAppId = oauthAppId.get,
      sessionId = sessionId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OAuthAppSessionRecord): OAuthAppSessionRecord =
    OAuthAppSessionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      oauthAppId = oauthAppId.getOrElse(record.oauthAppId),
      sessionId = sessionId.getOrElse(record.sessionId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
