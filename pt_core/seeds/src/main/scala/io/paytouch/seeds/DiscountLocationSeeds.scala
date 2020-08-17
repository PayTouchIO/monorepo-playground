package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object DiscountLocationSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val discountDao = daos.discountDao
  lazy val discountLocationDao = daos.discountLocationDao

  def load(
      discounts: Seq[DiscountRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[DiscountLocationRecord]] = {

    val discountLocations = discounts.flatMap { discount =>
      locations.random(LocationsPerDiscount).map { location =>
        DiscountLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          discountId = Some(discount.id),
          locationId = Some(location.id),
          active = None,
        )

      }
    }

    discountLocationDao.bulkUpsert(discountLocations).extractRecords
  }
}
