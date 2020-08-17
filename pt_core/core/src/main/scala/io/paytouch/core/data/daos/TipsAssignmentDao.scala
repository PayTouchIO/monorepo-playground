package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.TipsAssignmentsTable
import io.paytouch.core.entities.enums.HandledVia
import io.paytouch.core.filters.TipsAssignmentFilters

class TipsAssignmentDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickSoftDeleteDao {
  type Record = TipsAssignmentRecord
  type Update = TipsAssignmentUpdate
  type Filters = TipsAssignmentFilters
  type Table = TipsAssignmentsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      handledVia = f.handledVia,
      updatedSince = f.updatedSince,
    ).drop(offset).take(limit).result.pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      handledVia = f.handledVia,
      updatedSince = f.updatedSince,
    ).length.result.pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      handledVia: Option[HandledVia],
      updatedSince: Option[ZonedDateTime],
    ) =
    nonDeletedTable
      .filter { t =>
        all(
          Some(t.merchantId === merchantId),
          Some(t.locationId inSet locationIds),
          handledVia.map(hv => t.handledVia === hv),
          updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
        )
      }
      .sortBy(_.createdAt)

  def findByOrderIds(orderIds: Seq[UUID]): Future[Seq[Record]] =
    if (orderIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByOrderIds(orderIds)
        .result
        .pipe(run)

  private def queryFindByOrderIds(orderIds: Seq[UUID]) =
    nonDeletedTable.filter(_.orderId inSet orderIds)
}
