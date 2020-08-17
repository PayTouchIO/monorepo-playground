package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ CashDrawerActivityRecord, CashDrawerActivityUpdate }
import io.paytouch.core.data.tables.CashDrawerActivitiesTable
import io.paytouch.core.filters.CashDrawerActivityFilters

import scala.concurrent.ExecutionContext

class CashDrawerActivityDao(val cashDrawerDao: CashDrawerDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao {

  type Record = CashDrawerActivityRecord
  type Update = CashDrawerActivityUpdate
  type Table = CashDrawerActivitiesTable
  type Filters = CashDrawerActivityFilters

  val table = TableQuery[Table]

  def findByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    run(queryFindByIdsAndLocationIds(ids, locationIds).result)

  def queryFindByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    queryFindByIds(ids).filter(t => t.cashDrawerId in cashDrawerDao.queryFindByLocationIds(locationIds).map(_.id))

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int) =
    run(
      queryFindAllByMerchantIdAndCashDrawerId(merchantId, f.cashDrawerId, f.updatedSince)
        .drop(offset)
        .take(limit)
        .sortBy(_.createdAt)
        .result,
    )

  def countAllWithFilters(merchantId: UUID, filters: Filters) =
    run(queryFindAllByMerchantIdAndCashDrawerId(merchantId, filters.cashDrawerId).length.result)

  def findAllByMerchantIdAndCashDrawerId(merchantId: UUID, cashDrawerId: UUID) =
    run(queryFindAllByMerchantIdAndCashDrawerId(merchantId, cashDrawerId, None).result)

  def queryFindAllByMerchantIdAndCashDrawerId(
      merchantId: UUID,
      cashDrawerId: UUID,
      updatedSince: Option[ZonedDateTime] = None,
    ) =
    table.filter(t =>
      all(
        Some(t.cashDrawerId.map(_ === cashDrawerId).getOrElse(false)),
        Some(t.merchantId === merchantId),
        updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
      ),
    )
}
