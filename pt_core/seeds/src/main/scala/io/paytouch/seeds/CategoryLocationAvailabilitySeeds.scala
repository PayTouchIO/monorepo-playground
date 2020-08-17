package io.paytouch.seeds

import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.data.model.{ AvailabilityRecord, AvailabilityUpdate, CategoryLocationRecord, UserRecord }

import scala.concurrent._

object CategoryLocationAvailabilitySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val categoryLocationAvailabilityDao = daos.categoryLocationAvailabilityDao

  def load(
      categoryLocations: Seq[CategoryLocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[AvailabilityRecord]] = {

    val availabilities = categoryLocations.flatMap { categoryLocation =>
      (1 to AvailabilitiesPerCategoryLocation).map { _ =>
        val time1 = genLocalTime.instance
        val time2 = genLocalTime.instance
        AvailabilityUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          itemId = Some(categoryLocation.id),
          itemType = Some(AvailabilityItemType.CategoryLocation),
          sunday = Some(genBoolean.instance),
          monday = Some(genBoolean.instance),
          tuesday = Some(genBoolean.instance),
          wednesday = Some(genBoolean.instance),
          thursday = Some(genBoolean.instance),
          friday = Some(genBoolean.instance),
          saturday = Some(genBoolean.instance),
          start = Some(if (time1.isBefore(time2)) time1 else time2),
          end = Some(if (time1.isAfter(time2)) time1 else time2),
        )
      }
    }

    categoryLocationAvailabilityDao.bulkUpsert(availabilities).extractRecords
  }
}
