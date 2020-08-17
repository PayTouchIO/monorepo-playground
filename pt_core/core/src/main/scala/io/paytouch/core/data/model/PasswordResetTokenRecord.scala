package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class PasswordResetTokenRecord(
    id: UUID,
    userId: UUID,
    key: String,
    expiresAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

case class PasswordResetTokenUpdate(
    id: Option[UUID],
    userId: Option[UUID],
    key: Option[String],
    expiresAt: Option[ZonedDateTime],
  ) extends SlickUpdate[PasswordResetTokenRecord] {

  def toRecord: PasswordResetTokenRecord = {
    require(userId.isDefined, s"Impossible to convert PasswordResetTokenUpdate without a userId. [$this]")
    require(key.isDefined, s"Impossible to convert PasswordResetTokenUpdate without a key. [$this]")
    require(expiresAt.isDefined, s"Impossible to convert PasswordResetTokenUpdate without a expiresAt. [$this]")

    PasswordResetTokenRecord(
      id = id.getOrElse(UUID.randomUUID),
      userId = userId.get,
      key = key.get,
      expiresAt = expiresAt.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PasswordResetTokenRecord): PasswordResetTokenRecord =
    // Update is not supported
    record
}
