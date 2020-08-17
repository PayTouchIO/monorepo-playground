package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.{ Availabilities, AvailabilitiesPerItemId }
import io.paytouch.core.data.model.{ CategoryLocationRecord, CategoryLocationUpdate }
import io.paytouch.core.entities.{ CategoryLocation, UserContext }

trait CategoryLocationConversions {

  def fromItemLocationsToCategoryLocations(
      itemLocations: Seq[CategoryLocationRecord],
      availabilities: AvailabilitiesPerItemId,
    ) =
    itemLocations.map(itemLoc => fromItemLocationToCategoryLocation(itemLoc, availabilities.get(itemLoc.locationId)))

  def fromItemLocationToCategoryLocation(itemLocation: CategoryLocationRecord, availabilities: Option[Availabilities]) =
    itemLocation.locationId -> CategoryLocation(active = Some(itemLocation.active), availabilities = availabilities)

  def toCategoryLocationUpdates(
      categoryIds: Seq[UUID],
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Seq[CategoryLocationUpdate] =
    categoryIds.map(toCategoryLocationUpdate(_, locationId))

  def toCategoryLocationUpdates(
      categoryId: UUID,
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[CategoryLocationUpdate] =
    locationIds.map(toCategoryLocationUpdate(categoryId, _))

  def toCategoryLocationUpdate(categoryId: UUID, locationId: UUID)(implicit user: UserContext): CategoryLocationUpdate =
    CategoryLocationUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(user.merchantId),
      categoryId = Some(categoryId),
      locationId = Some(locationId),
      active = None,
    )

  def toCategoryLocationUpdate(record: CategoryLocationRecord)(implicit user: UserContext): CategoryLocationUpdate =
    CategoryLocationUpdate(
      id = Some(record.id),
      merchantId = Some(user.merchantId),
      categoryId = Some(record.categoryId),
      locationId = Some(record.locationId),
      active = Some(record.active),
    )
}
