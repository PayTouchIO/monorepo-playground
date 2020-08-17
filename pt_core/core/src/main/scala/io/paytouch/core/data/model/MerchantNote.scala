package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class MerchantNote(
    id: UUID,
    userId: UUID,
    body: String,
    createdAt: ZonedDateTime,
  ) {

  // TODO - can we do better?
  override def equals(that: Any): Boolean =
    that match {
      case that: MerchantNote =>
        id == that.id && userId == userId && body == body && createdAt.isEqual(that.createdAt)
      case _ => false
    }
}
