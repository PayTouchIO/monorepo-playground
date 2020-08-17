package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.LoginSource

final case class SessionRecord(
    id: UUID,
    userId: UUID,
    jti: String,
    source: LoginSource,
    adminId: Option[UUID],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord
