package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

final case class MerchantNote(
    id: UUID,
    user: Option[UserInfo],
    body: String,
    createdAt: ZonedDateTime,
  )

final case class MerchantNoteUpsertion(
    id: UUID,
    userId: UUID,
    body: String,
    createdAt: ZonedDateTime,
  )
