package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext

final case class PayrollFilters(
    locationIds: Seq[UUID] = Seq.empty,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    query: Option[String] = None,
  ) extends BaseFilters

object PayrollFilters {
  def withAccessibleLocations(
      locationId: Option[UUID] = None,
      from: Option[LocalDateTime] = None,
      to: Option[LocalDateTime] = None,
      query: Option[String] = None,
    )(implicit
      user: UserContext,
    ) = {
    val locationIds = user.accessibleLocations(locationId)
    PayrollFilters(locationIds, from, to, query)
  }
}
