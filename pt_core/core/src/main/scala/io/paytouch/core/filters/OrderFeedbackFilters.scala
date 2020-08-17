package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.FeedbackStatus

final case class OrderFeedbackFilters(
    locationId: Option[UUID] = None,
    customerId: Option[UUID] = None,
    status: Option[FeedbackStatus] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
  ) extends BaseFilters {
  val read: Option[Boolean] = status.map(_ == FeedbackStatus.Read)
}
