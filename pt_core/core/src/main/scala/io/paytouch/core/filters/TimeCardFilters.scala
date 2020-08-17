package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext
import io.paytouch.core.entities.enums.TimeCardStatus

final case class TimeCardFilters(
    locationIds: Seq[UUID] = Seq.empty,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    status: Option[TimeCardStatus] = None,
    query: Option[String] = None,
  ) extends BaseFilters

object TimeCardFilters {

  def withAccessibleLocations(
      locationId: Option[UUID],
      startDate: Option[LocalDateTime],
      endDate: Option[LocalDateTime],
      status: Option[TimeCardStatus],
      query: Option[String],
    )(implicit
      user: UserContext,
    ): TimeCardFilters = {
    val locationIds = user.accessibleLocations(locationId)
    TimeCardFilters(locationIds, startDate, endDate, status, query)
  }
}
