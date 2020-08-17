package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class TaxRateFilters(locationIds: Option[Seq[UUID]] = None, updatedSince: Option[ZonedDateTime] = None)
    extends BaseFilters
