package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OAuthAppRecord(
    id: UUID,
    clientId: UUID,
    clientSecret: UUID,
    name: String,
    redirectUris: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

case class OAuthAppUpdate(
    id: Option[UUID],
    clientId: Option[UUID],
    clientSecret: Option[UUID],
    name: Option[String],
    redirectUris: Option[String],
  ) extends SlickUpdate[OAuthAppRecord] {

  def updateRecord(record: OAuthAppRecord): OAuthAppRecord =
    OAuthAppRecord(
      id = id.getOrElse(record.id),
      clientId = clientId.getOrElse(record.clientId),
      clientSecret = clientSecret.getOrElse(record.clientSecret),
      name = name.getOrElse(record.name),
      redirectUris = redirectUris.getOrElse(record.redirectUris),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: OAuthAppRecord = {
    require(clientId.isDefined, s"Impossible to convert OAuthAppUpdate without a client id. [$this]")
    require(clientSecret.isDefined, s"Impossible to convert OAuthAppUpdate without a client secret. [$this]")
    require(name.isDefined, s"Impossible to convert OAuthAppUpdate without a oauth redirectUris id. [$this]")
    require(redirectUris.isDefined, s"Impossible to convert OAuthAppUpdate without a redirectUris. [$this]")
    OAuthAppRecord(
      id = id.getOrElse(UUID.randomUUID),
      clientId = clientId.get,
      clientSecret = clientSecret.get,
      name = name.get,
      redirectUris = redirectUris.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
