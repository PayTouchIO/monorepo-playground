package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext

final case class KitchenFilters(locationIds: Seq[UUID] = Seq.empty, updatedSince: Option[ZonedDateTime] = None)
    extends BaseFilters

object KitchenFilters {
  def withAccessibleLocations(
      locationId: Option[UUID] = None,
      updatedSince: Option[ZonedDateTime] = None,
    )(implicit
      user: UserContext,
    ): KitchenFilters = {
    val locationIds = user.accessibleLocations(locationId)
    KitchenFilters(locationIds, updatedSince)
  }
}
