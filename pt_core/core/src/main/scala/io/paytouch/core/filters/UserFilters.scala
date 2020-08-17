package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.UserContext

final case class UserFilters(
    locationIds: Seq[UUID] = Seq.empty,
    userRoleId: Option[UUID] = None,
    query: Option[String] = None,
    updatedSince: Option[ZonedDateTime] = None,
  ) extends BaseFilters

object UserFilters {
  def withAccessibleLocations(
      locationId: Option[UUID] = None,
      userRoleId: Option[UUID] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime] = None,
    )(implicit
      user: UserContext,
    ) = {
    val locationIds = user.accessibleLocations(locationId)
    UserFilters(locationIds, userRoleId, query, updatedSince)
  }
}
