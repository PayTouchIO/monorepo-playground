package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class CashDrawerActivityFilters(cashDrawerId: UUID, updatedSince: Option[ZonedDateTime] = None)
    extends BaseFilters
