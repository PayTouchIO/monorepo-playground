package io.paytouch.ordering.data.daos.features

import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.SlickSoftDeleteRecord
import io.paytouch.ordering.data.tables.features.{ DeletedAtColumn, LocationIdColumn, SlickTable }
import io.paytouch.ordering.utils.UtcTime
import slick.lifted.{ CanBeQueryCondition, Rep }

import scala.concurrent.Future

trait SlickSoftDeleteDao extends SlickLocationDao {

  type Record <: SlickSoftDeleteRecord
  type Table <: SlickTable[Record] with DeletedAtColumn with LocationIdColumn

  def nonDeletedTable = table.filter(_.deletedAt.isEmpty)

  override def queryFindAllByLocationIds(locationIds: Seq[UUID]) =
    nonDeletedTable.filter(t => t.locationId inSet locationIds)

  override def queryDeleteByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) = {
    val field =
      for { o <- table if o.id.inSet(ids) && o.locationId.inSet(locationIds) } yield (
        o.deletedAt,
        o.updatedAt,
      )
    val deletionTime = Some(UtcTime.now)
    field.update(deletionTime, UtcTime.now).map(_ => ids)
  }

  override def queryByIds(ids: Seq[UUID]) = nonDeletedTable.filter(idColumnSelector(_) inSet ids)

  def findDeletedById(id: UUID): Future[Option[Record]] = findDeletedByIds(Seq(id)).map(_.headOption)

  def findDeletedByIds(ids: Seq[UUID]): Future[Seq[Record]] = run(queryDeletedByIds(ids).result)

  def queryDeletedByIds(ids: Seq[UUID]) = table.filter(idColumnSelector(_) inSet ids)

  override def queryDeleteTheRestByDeleteFilter[R <: Rep[_]](
      validEntities: Seq[Record],
      deleteFilter: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ) = {
    val field = for {
      o <- table.filter(deleteFilter).filterNot(idColumnSelector(_) inSet validEntities.map(_.id))
    } yield (o.deletedAt, o.updatedAt)
    val deletionTime = Some(UtcTime.now)
    field.update(deletionTime, UtcTime.now)
  }
}
