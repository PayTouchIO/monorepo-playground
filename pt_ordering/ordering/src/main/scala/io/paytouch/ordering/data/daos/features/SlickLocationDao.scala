package io.paytouch.ordering.data.daos.features

import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.SlickLocationRecord
import io.paytouch.ordering.data.tables.features.{ LocationIdColumn, SlickTable }
import slick.lifted.Ordered

import scala.concurrent.Future

trait SlickLocationDao extends SlickDao {

  type Record <: SlickLocationRecord
  type Table <: SlickTable[Record] with LocationIdColumn

  def findAllByLocationIds(
      locationIds: Seq[UUID],
      ordering: Table => Ordered = _.createdAt,
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q = queryFindAllByLocationIds(locationIds).sortBy(ordering).drop(offset).take(limit)
    run(q.result)
  }

  def countAllByLocationIds(locationIds: Seq[UUID]): Future[Int] =
    run(queryFindAllByLocationIds(locationIds).length.result)

  def deleteByIdAndLocationIds(id: UUID, locationIds: Seq[UUID]): Future[UUID] =
    deleteByIdsAndLocationIds(Seq(id), locationIds).map(_ => id)

  def deleteByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]): Future[Seq[UUID]] = {
    if (ids.isEmpty) return Future.successful(Seq.empty)

    runWithTransaction(queryDeleteByIdsAndLocationIds(ids, locationIds))
  }

  def queryFindAllByLocationIds(locationIds: Seq[UUID]) = table.filter(_.locationId inSet locationIds)

  def queryDeleteByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    queryFindAllByLocationIds(locationIds).filter(idColumnSelector(_) inSet ids).delete.map(_ => ids)
}
