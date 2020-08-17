package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ DiscountLocationRecord, DiscountLocationUpdate }
import io.paytouch.core.data.tables.DiscountLocationsTable

import scala.concurrent.ExecutionContext

class DiscountLocationDao(
    discountDao: => DiscountDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationToggleableDao {

  type Record = DiscountLocationRecord
  type Update = DiscountLocationUpdate
  type Table = DiscountLocationsTable

  val table = TableQuery[Table]

  val itemDao = discountDao
  implicit val l: LocationDao = locationDao

  def queryByRelIds(discountLocationUpdate: Update) = {
    require(
      discountLocationUpdate.discountId.isDefined,
      "DiscountLocationDao - Impossible to find by discount id and location id without a discount id",
    )
    require(
      discountLocationUpdate.locationId.isDefined,
      "DiscountLocationDao - Impossible to find by discount id and location id without a location id",
    )
    queryFindByItemIdAndLocationId(discountLocationUpdate.discountId.get, discountLocationUpdate.locationId.get)
  }

  def queryFindByDiscountIdAndLocationIds(discountId: UUID, locationIds: Seq[UUID]) =
    table.filter(_.discountId === discountId).filterByLocationIds(locationIds)
}
