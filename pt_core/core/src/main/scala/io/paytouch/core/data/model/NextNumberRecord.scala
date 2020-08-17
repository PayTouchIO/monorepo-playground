package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ NextNumberType, ScopeType }
import io.paytouch.core.entities.ScopeKey

final case class NextNumberRecord(
    id: UUID,
    scopeType: ScopeType,
    scopeKey: ScopeKey,
    `type`: NextNumberType,
    nextVal: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord
