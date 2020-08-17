package io.paytouch.core.filters

import java.time.LocalDate
import java.util.UUID

import io.paytouch.core.data.model.enums.ShiftStatus
import io.paytouch.core.entities.UserContext

final case class ShiftFilters(
    locationIds: Seq[UUID] = Seq.empty,
    from: Option[LocalDate] = None,
    to: Option[LocalDate] = None,
    userRoleId: Option[UUID] = None,
    status: Option[ShiftStatus] = None,
  ) extends BaseFilters

object ShiftFilters {
  def withAccessibleLocations(
      locationId: Option[UUID] = None,
      from: Option[LocalDate] = None,
      to: Option[LocalDate] = None,
      userRoleId: Option[UUID] = None,
      status: Option[ShiftStatus] = None,
    )(implicit
      user: UserContext,
    ) = {
    val locationIds = user.accessibleLocations(locationId)
    ShiftFilters(locationIds, from, to, userRoleId, status)
  }
}
