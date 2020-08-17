package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext
import io.paytouch.core.entities.enums.HandledVia

final case class TipsAssignmentFilters(
    locationIds: Seq[UUID] = Seq.empty,
    handledVia: Option[HandledVia] = None,
    updatedSince: Option[ZonedDateTime] = None,
  ) extends BaseFilters

object TipsAssignmentFilters {
  def withAccessibleLocations(
      locationId: UUID,
      handledVia: Option[HandledVia] = None,
      updatedSince: Option[ZonedDateTime] = None,
    )(implicit
      user: UserContext,
    ) = {
    val locationIds = user.accessibleLocations(Some(locationId))
    TipsAssignmentFilters(locationIds, handledVia, updatedSince)
  }
}
