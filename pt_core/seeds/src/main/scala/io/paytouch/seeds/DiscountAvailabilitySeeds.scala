package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.AvailabilityItemType

import scala.concurrent._

object DiscountAvailabilitySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val discountAvailabilityDao = daos.discountAvailabilityDao

  def load(discounts: Seq[DiscountRecord])(implicit user: UserRecord): Future[Seq[AvailabilityRecord]] = {

    val availabilities = discounts.flatMap { discount =>
      (1 to AvailabilitiesPerDiscount).map { _ =>
        val time1 = genLocalTime.instance
        val time2 = genLocalTime.instance
        AvailabilityUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          itemId = Some(discount.id),
          itemType = Some(AvailabilityItemType.Discount),
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

    discountAvailabilityDao.bulkUpsert(availabilities).extractRecords
  }
}
