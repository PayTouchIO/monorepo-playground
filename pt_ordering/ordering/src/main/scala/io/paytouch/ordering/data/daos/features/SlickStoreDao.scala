package io.paytouch.ordering.data.daos.features

import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.SlickStoreRecord
import io.paytouch.ordering.data.tables.features.SlickStoreTable
import slick.lifted.Ordered

import scala.concurrent.Future

trait SlickStoreDao extends SlickDao {

  type Record <: SlickStoreRecord
  type Table <: SlickStoreTable[Record]

  def findAllByStoreIds(
      storeIds: Seq[UUID],
      ordering: Table => Ordered = _.createdAt,
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q = queryFindAllByStoreIds(storeIds).sortBy(ordering).drop(offset).take(limit)
    run(q.result)
  }

  def countAllByStoreIds(storeIds: Seq[UUID]): Future[Int] =
    run(queryFindAllByStoreIds(storeIds).length.result)

  def deleteByIdAndStoreIds(id: UUID, storeIds: Seq[UUID]): Future[UUID] =
    deleteByIdsAndStoreIds(Seq(id), storeIds).map(_ => id)

  def deleteByIdsAndStoreIds(ids: Seq[UUID], storeIds: Seq[UUID]): Future[Seq[UUID]] = {
    if (ids.isEmpty) return Future.successful(Seq.empty)

    runWithTransaction(queryDeleteByIdsAndStoreIds(ids, storeIds))
  }

  def findByIdsAndStoreId(ids: Seq[UUID], storeId: UUID): Future[Seq[Record]] = {
    val q = queryFindByIdsAndStoreIds(ids = ids, storeIds = Seq(storeId))
    run(q.result)
  }

  def queryDeleteByIdsAndStoreIds(ids: Seq[UUID], storeIds: Seq[UUID]) =
    queryFindByIdsAndStoreIds(ids = ids, storeIds = storeIds).delete.map(_ => ids)

  def queryFindByIdsAndStoreIds(ids: Seq[UUID], storeIds: Seq[UUID]) =
    queryFindAllByStoreIds(storeIds).filter(idColumnSelector(_) inSet ids)

  def queryFindAllByStoreIds(storeIds: Seq[UUID]) = table.filter(_.storeId inSet storeIds)
}
