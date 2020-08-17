package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext

final case class TimeOffCardFilters(
    locationIds: Seq[UUID] = Seq.empty,
    query: Option[String] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
  ) extends BaseFilters

object TimeOffCardFilters {
  def withAccessibleLocations(
      locationId: Option[UUID] = None,
      query: Option[String] = None,
      from: Option[LocalDateTime] = None,
      to: Option[LocalDateTime] = None,
    )(implicit
      user: UserContext,
    ) = {
    val locationIds = user.accessibleLocations(locationId)
    TimeOffCardFilters(locationIds, query, from, to)
  }
}
