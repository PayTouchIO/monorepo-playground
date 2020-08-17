package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OAuthCodeRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    oauthAppId: UUID,
    code: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OAuthCodeUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: Option[UUID],
    oauthAppId: Option[UUID],
    code: Option[UUID],
  ) extends SlickMerchantUpdate[OAuthCodeRecord] {

  def updateRecord(record: OAuthCodeRecord): OAuthCodeRecord =
    OAuthCodeRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      oauthAppId = oauthAppId.getOrElse(record.oauthAppId),
      code = code.getOrElse(record.code),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: OAuthCodeRecord = {
    require(merchantId.isDefined, s"Impossible to convert OAuthCodeUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert OAuthCodeUpdate without a user id. [$this]")
    require(oauthAppId.isDefined, s"Impossible to convert OAuthCodeUpdate without a oauth app id. [$this]")
    require(code.isDefined, s"Impossible to convert OAuthCodeUpdate without a code. [$this]")
    OAuthCodeRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      oauthAppId = oauthAppId.get,
      code = code.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
