package io.paytouch.seeds

import scala.concurrent._

import io.paytouch.core.data.model.{ LocationRecord, LocationUpdate, UserRecord }
import io.paytouch.core.entities.{ Coordinates, ResettableString }
import io.paytouch.seeds.IdsProvider._
import io.paytouch.seeds.utils.RealLocationHelper

object LocationSeeds extends Seeds {
  lazy val locationDao = daos.locationDao

  def load(implicit user: UserRecord): Future[Seq[LocationRecord]] = {
    val locationIds = locationIdsPerEmail(user.email)

    val locations = locationIds.zip(RealLocationHelper.locations.shuffle).map {
      case (locationId, realLocation) =>
        val address = realLocation.address
        val store = realLocation.name
        LocationUpdate(
          id = Some(locationId),
          merchantId = Some(user.merchantId),
          name = Some(store),
          email = Some(s"${store.toLowerCase.replaceAll(" ", "-")}@paytouch-test.io"),
          phoneNumber = s"+$randomNumericString",
          website = s"https://www.paytouch.io",
          addressLine1 = Some(address.line1),
          addressLine2 = Some(address.line2),
          city = Some(address.city),
          state = Some(address.state),
          country = address.country,
          stateCode = address.stateData.map(_.code),
          countryCode = address.countryData.map(_.code),
          postalCode = Some(address.postalCode),
          timezone = Some(realLocation.timezone),
          active = None,
          dummyData = None,
          latitude = None,
          longitude = None,
          deletedAt = None,
        )
    }

    locationDao.bulkUpsert(locations).extractRecords
  }
}
