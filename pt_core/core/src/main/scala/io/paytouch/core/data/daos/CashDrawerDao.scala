package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.upsertions.CashDrawerUpsertion
import io.paytouch.core.data.model.{ CashDrawerRecord, CashDrawerUpdate }
import io.paytouch.core.data.tables.CashDrawersTable
import io.paytouch.core.filters.CashDrawerFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime

import scala.concurrent._

class CashDrawerDao(
    cashDrawerActivityDao: => CashDrawerActivityDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao {

  type Record = CashDrawerRecord
  type Update = CashDrawerUpdate
  type Table = CashDrawersTable
  type Filters = CashDrawerFilters
  type Upsertion = CashDrawerUpsertion

  val table = TableQuery[Table]

  implicit val l: LocationDao = locationDao

  def findAllWithFilters(merchantId: UUID, f: CashDrawerFilters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationIds, f.updatedSince)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    run(
      queryFindAllByMerchantId(merchantId, locationIds, updatedSince)
        .sortBy(_.createdAt)
        .drop(offset)
        .take(limit)
        .result,
    )

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      updatedSince: Option[ZonedDateTime],
    ) =
    table
      .filterByLocationIds(locationIds)
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
        ),
      )

  def countAllWithFilters(merchantId: UUID, f: CashDrawerFilters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationIds, f.updatedSince)

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationIds, updatedSince).length.result)

  def queryFindByLocationIds(locationIds: Seq[UUID]) =
    table.filterByLocationIds(locationIds)

  def storeExportS3Filename(id: UUID, filename: String): Future[Boolean] = {
    val field = for { o <- table if o.id === id } yield (o.exportFilename, o.updatedAt)
    run(field.update(Some(filename), UtcTime.now).map(_ > 0))
  }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val upserts = for {
      (resultType, cashDrawer) <- queryUpsert(upsertion.cashDrawerUpdate)
      _ <- cashDrawerActivityDao.queryBulkUpsert(upsertion.cashDrawerActivities)
    } yield (resultType, cashDrawer)
    runWithTransaction(upserts)
  }
}
