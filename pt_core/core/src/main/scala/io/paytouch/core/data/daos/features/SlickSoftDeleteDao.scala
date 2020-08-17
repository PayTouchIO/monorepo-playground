package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ SlickSoftDeleteRecord, SlickSoftDeleteUpdate }
import io.paytouch.core.data.tables.SlickSoftDeleteTable
import io.paytouch.core.utils.UtcTime
import slick.lifted.{ CanBeQueryCondition, Rep }

import scala.concurrent._

trait SlickSoftDeleteDao extends SlickMerchantDao {
  type Record <: SlickSoftDeleteRecord
  type Update <: SlickSoftDeleteUpdate[Record]
  type Table <: SlickSoftDeleteTable[Record]

  def nonDeletedTable = baseQuery.filter(_.deletedAt.isEmpty)

  override def queryFindAllByMerchantIds(merchantIds: Seq[UUID]) =
    nonDeletedTable.filter(t => t.merchantId inSet merchantIds)

  override def queryDeleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID) = {
    val now = UtcTime.now

    table
      .filter(_.id inSet ids)
      .filter(_.merchantId === merchantId)
      .map(o => o.deletedAt -> o.updatedAt)
      .update(Some(now), now)
      .map(_ => ids)
  }

  override def queryByIds(ids: Seq[UUID]) = nonDeletedTable.filter(idColumnSelector(_) inSet ids)

  def findDeletedById(id: UUID): Future[Option[Record]] = findDeletedByIds(Seq(id)).map(_.headOption)

  def findDeletedByIds(ids: Seq[UUID]): Future[Seq[Record]] = run(queryDeletedByIds(ids).result)

  def queryDeletedByIds(ids: Seq[UUID]) = baseQuery.filter(idColumnSelector(_) inSet ids)

  override def queryDeleteTheRestByDeleteFilter[R <: Rep[_]](
      validEntities: Seq[Record],
      deleteFilter: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ) = {
    val now = UtcTime.now

    table
      .filter(deleteFilter)
      .filterNot(idColumnSelector(_) inSet validEntities.map(_.id))
      .map(o => o.deletedAt -> o.updatedAt)
      .update(Some(now), now)
  }
}
