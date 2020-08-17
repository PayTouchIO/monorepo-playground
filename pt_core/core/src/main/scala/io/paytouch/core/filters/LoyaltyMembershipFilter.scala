package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

final case class LoyaltyMembershipFilter(
    loyaltyProgramId: Option[UUID] = None,
    updatedSince: Option[ZonedDateTime] = None,
  ) extends BaseFilters
