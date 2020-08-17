package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.AvailabilitiesPerItemId
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.data.model.{ AvailabilityRecord, CategoryLocationRecord }
import io.paytouch.core.entities.UserContext

trait CategoryLocationAvailabilityConversions extends AvailabilityConversions {

  def itemType = AvailabilityItemType.CategoryLocation

  def groupAvailabilitiesPerCategory(
      categoryLocations: Seq[CategoryLocationRecord],
      locationAvailabilities: Seq[AvailabilityRecord],
    )(implicit
      user: UserContext,
    ): Map[UUID, AvailabilitiesPerItemId] =
    categoryLocations.groupBy(_.categoryId).transform { (_, catLocs) =>
      catLocs.map { categoryLocation =>
        val locationAvailabilityPerCatLoc =
          locationAvailabilities
            .filter(la => la.itemId == categoryLocation.id && la.itemType == AvailabilityItemType.CategoryLocation)

        categoryLocation.locationId -> toAvailabilityMap(locationAvailabilityPerCatLoc)
      }.toMap
    }
}
