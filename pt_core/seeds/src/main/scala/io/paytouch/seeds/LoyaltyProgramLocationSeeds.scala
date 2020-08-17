package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object LoyaltyProgramLocationSeeds extends Seeds {

  lazy val loyaltyProgramLocationDao = daos.loyaltyProgramLocationDao

  def load(
      loyaltyPrograms: Seq[LoyaltyProgramRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[LoyaltyProgramLocationRecord]] = {

    val loyaltyProgramLocations = loyaltyPrograms.flatMap { loyaltyProgram =>
      locations.randomAtLeast(2).map { location =>
        LoyaltyProgramLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          locationId = Some(location.id),
          loyaltyProgramId = Some(loyaltyProgram.id),
        )
      }
    }

    loyaltyProgramLocationDao.bulkUpsertByRelIds(loyaltyProgramLocations).extractRecords
  }
}
