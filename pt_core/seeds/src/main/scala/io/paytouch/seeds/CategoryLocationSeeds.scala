package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object CategoryLocationSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val categoryLocationDao = daos.categoryLocationDao

  def load(
      categories: Seq[CategoryRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[CategoryLocationRecord]] = {

    val categoryLocations = categories.flatMap { category =>
      locations.random(LocationsPerCategory).map { location =>
        CategoryLocationUpdate(
          id = None,
          merchantId = Some(location.merchantId),
          locationId = Some(location.id),
          categoryId = Some(category.id),
          active = None,
        )
      }
    }

    categoryLocationDao.bulkUpsertByRelIds(categoryLocations).extractRecords
  }
}
