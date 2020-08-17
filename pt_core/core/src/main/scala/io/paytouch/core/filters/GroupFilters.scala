package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

final case class GroupFilters(
    locationId: Option[UUID] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    query: Option[String] = None,
  ) extends BaseFilters
