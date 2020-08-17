package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderStatus
import io.paytouch.core.utils.UtcTime

final case class StatusTransition(
    id: UUID,
    status: OrderStatus,
    createdAt: ZonedDateTime,
  ) {

  // TODO - can we do better?
  override def equals(that: Any): Boolean =
    that match {
      case that: MerchantNote =>
        id == that.id && status == status && createdAt.isEqual(that.createdAt)
      case _ => false
    }
}

object StatusTransition {

  def apply(status: OrderStatus): StatusTransition = apply(UUID.randomUUID, status, UtcTime.now)
}
