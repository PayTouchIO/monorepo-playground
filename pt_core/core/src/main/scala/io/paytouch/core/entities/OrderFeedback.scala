package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class OrderFeedback(
    id: UUID,
    orderId: UUID,
    customerId: UUID,
    rating: Int,
    body: String,
    read: Boolean,
    receivedAt: ZonedDateTime,
    customer: Option[CustomerMerchant],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.OrderFeedback
}
