package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class StockFilters(
    productId: Option[UUID] = None,
    locationIds: Option[Seq[UUID]] = None,
    updatedSince: Option[ZonedDateTime],
  ) extends BaseFilters
