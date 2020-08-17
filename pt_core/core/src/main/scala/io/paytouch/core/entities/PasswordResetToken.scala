package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class PasswordResetToken(
    id: UUID,
    userId: UUID,
    key: String,
    expiresAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.PasswordResetToken

}
