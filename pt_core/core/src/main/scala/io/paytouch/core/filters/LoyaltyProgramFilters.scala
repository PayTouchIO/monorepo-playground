package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class LoyaltyProgramFilters(locationId: Option[UUID] = None, updatedSince: Option[ZonedDateTime] = None)
    extends BaseFilters
