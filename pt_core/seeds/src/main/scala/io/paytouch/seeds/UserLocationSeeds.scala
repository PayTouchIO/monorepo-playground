package io.paytouch.seeds

import io.paytouch.core.data.model.{ LocationRecord, UserLocationRecord, UserLocationUpdate, UserRecord }

import scala.concurrent._

object UserLocationSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val userLocationDao = daos.userLocationDao

  def load(
      locations: Seq[LocationRecord],
      users: Seq[UserRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[UserLocationRecord]] = {

    val userLocations = users.flatMap { user =>
      val selectedLocations = if (user.isOwner) locations else locations.random(LocationsPerUser)
      selectedLocations.map { location =>
        UserLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          locationId = Some(location.id),
          userId = Some(user.id),
        )
      }
    }

    userLocationDao.bulkUpsertByRelIds(userLocations).extractRecords
  }
}
