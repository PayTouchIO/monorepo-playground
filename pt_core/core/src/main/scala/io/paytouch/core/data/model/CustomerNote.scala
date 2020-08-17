package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class CustomerNote(
    id: UUID,
    body: String,
    createdAt: ZonedDateTime,
  ) {

  // TODO - can we do better?
  override def equals(that: Any): Boolean =
    that match {
      case that: CustomerNote =>
        id == that.id && body == body && createdAt.isEqual(that.createdAt)
      case _ => false
    }
}
