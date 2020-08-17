package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ReturnOrderStatus

final case class ReturnOrderFilters(
    locationId: Option[UUID] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    query: Option[String] = None,
    status: Option[ReturnOrderStatus] = None,
  ) extends BaseFilters
