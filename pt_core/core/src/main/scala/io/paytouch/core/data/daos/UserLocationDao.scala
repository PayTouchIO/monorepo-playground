package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickItemLocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ UserLocationRecord, UserLocationUpdate }
import io.paytouch.core.data.tables.UserLocationsTable

import scala.concurrent._

class UserLocationDao(
    val locationDao: LocationDao,
    timeOffCardDao: => TimeOffCardDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationDao {

  type Record = UserLocationRecord
  type Update = UserLocationUpdate
  type Table = UserLocationsTable

  val table = TableQuery[Table]

  def queryByRelIds(userLocationUpdate: Update) = {
    require(
      userLocationUpdate.userId.isDefined,
      "UserLocationDao - Impossible to find by user id and location id without a user id",
    )
    require(
      userLocationUpdate.locationId.isDefined,
      "UserLocationDao - Impossible to find by user id and location id without a location id",
    )
    queryFindByItemIdAndLocationId(userLocationUpdate.userId.get, userLocationUpdate.locationId.get)
  }

  def findByTimeOffCardIds(timeOffCardIds: Seq[UUID]): Future[Seq[Record]] = {
    val q = table.filter(_.userId in timeOffCardDao.queryFindByIds(timeOffCardIds).map(_.userId))
    run(q.result)
  }
}
