package io.paytouch.core.data.queries

import java.util.UUID

import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OptLocationIdColumn
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.data.tables.SlickTable

class OptLocationIdQuery[R <: SlickRecord, T <: SlickTable[R] with OptLocationIdColumn](
    q: Query[T, T#TableElementType, Seq],
  )(implicit
    locationDao: LocationDao,
  ) {

  def filterByLocationId(locationId: UUID) = filterByLocationIds(Seq(locationId))

  def filterByLocationIds(locationIds: Seq[UUID]) =
    q.filter(_.locationId in locationDao.queryByIds(locationIds).map(_.id))
}
