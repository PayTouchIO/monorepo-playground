package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickLocationOptTimeZoneHelper }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderFeedbackRecord, OrderFeedbackUpdate }
import io.paytouch.core.data.tables.OrderFeedbacksTable
import io.paytouch.core.filters.OrderFeedbackFilters

import scala.concurrent._

class OrderFeedbackDao(
    val locationDao: LocationDao,
    val orderDao: OrderDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickLocationOptTimeZoneHelper {

  type Record = OrderFeedbackRecord
  type Update = OrderFeedbackUpdate
  type Filters = OrderFeedbackFilters
  type Table = OrderFeedbacksTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationId, f.customerId, f.read, f.from, f.to)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      customerId: Option[UUID],
      read: Option[Boolean],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    run(
      queryFindAllByMerchantId(merchantId, locationId, customerId, read, from, to)
        .sortBy(r => (r.read.asc, r.receivedAt.asc))
        .drop(offset)
        .take(limit)
        .result,
    )

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.customerId, f.read, f.from, f.to)

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      customerId: Option[UUID],
      read: Option[Boolean],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationId, customerId, read, from, to).length.result)

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      customerId: Option[UUID],
      read: Option[Boolean],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    table.filter(t =>
      all(
        Some(t.merchantId === merchantId),
        locationId.map(lId => t.orderId in orderDao.queryFindByLocationId(lId).map(_.id)),
        customerId.map(cId => t.customerId === cId),
        read.map(r => t.read === r),
        from.map(start => t.id in itemIdsAtOrAfterDate(start)(_.receivedAt)),
        to.map(end => t.id in itemIdsBeforeDate(end)(_.receivedAt)),
      ),
    )

}
