package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext

final case class CashDrawerFilters(locationIds: Seq[UUID] = Seq.empty, updatedSince: Option[ZonedDateTime] = None)
    extends BaseFilters

object CashDrawerFilters {
  def withAccessibleLocations(
      locationId: Option[UUID] = None,
      updatedSince: Option[ZonedDateTime] = None,
    )(implicit
      user: UserContext,
    ) = {
    val locationIds = user.accessibleLocations(locationId)
    CashDrawerFilters(locationIds, updatedSince)
  }
}
