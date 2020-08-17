package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ReceivingOrderStatus

final case class ReceivingOrderFilters(
    locationId: Option[UUID] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    query: Option[String] = None,
    status: Option[ReceivingOrderStatus] = None,
  ) extends BaseFilters
