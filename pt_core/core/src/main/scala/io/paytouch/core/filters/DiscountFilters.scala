package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class DiscountFilters(
    locationIds: Option[Seq[UUID]] = None,
    query: Option[String] = None,
    updatedSince: Option[ZonedDateTime] = None,
  ) extends BaseFilters
