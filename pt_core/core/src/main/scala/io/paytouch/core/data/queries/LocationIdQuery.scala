package io.paytouch.core.data.queries

import java.util.UUID

import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.LocationIdColumn
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.data.tables.SlickTable

class LocationIdQuery[R <: SlickRecord, T <: SlickTable[R] with LocationIdColumn](
    q: Query[T, T#TableElementType, Seq],
  )(implicit
    locationDao: LocationDao,
  ) {

  def filterByLocationIds(locationIds: Seq[UUID]) =
    q.filter(_.locationId in locationDao.queryByIds(locationIds).map(_.id))

  def filterByLocationId(locationId: UUID) = filterByLocationIds(Seq(locationId))
}
