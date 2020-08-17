package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ TaxRateRecord, TaxRateUpdate }
import io.paytouch.core.data.model.upsertions.TaxRateUpsertion
import io.paytouch.core.data.tables.TaxRatesTable
import io.paytouch.core.filters.TaxRateFilters
import io.paytouch.core.utils.ResultType

class TaxRateDao(taxRateLocationDao: => TaxRateLocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao {
  type Record = TaxRateRecord
  type Update = TaxRateUpdate
  type Upsertion = TaxRateUpsertion
  type Filters = TaxRateFilters
  type Table = TaxRatesTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationIds, f.updatedSince)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationIds, f.updatedSince)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]] = None,
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId, locationIds, updatedSince)
      .sortBy(_.name.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]] = None,
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    queryFindAllByMerchantId(merchantId, locationIds, updatedSince)
      .length
      .result
      .pipe(run)

  def findAllByMerchantIdAndLocationIds(merchantId: UUID, locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindAllByMerchantId(merchantId, Some(locationIds), updatedSince = None)
        .result
        .pipe(run)

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]],
      updatedSince: Option[ZonedDateTime],
    ) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        locationIds.map(locIds => t.id in taxRateLocationDao.queryFindByLocationIds(locIds).map(_.taxRateId)),
        updatedSince.map { date =>
          any(
            t.id in queryUpdatedSince(date).map(_.id),
            t.id in taxRateLocationDao.queryUpdatedSince(date).map(_.taxRateId),
          )
        },
      )
    }

  def findByNamesAndMerchantId(names: Seq[String], merchantId: UUID): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => t.name.toLowerCase.inSet(names.map(_.toLowerCase)) && t.merchantId === merchantId)
        .result
        .pipe(run)

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      tuple @ (resultType, taxRate) <- queryUpsert(upsertion.taxRate)
      taxRateLocations <- taxRateLocationDao.queryBulkUpsertAndDeleteTheRest(upsertion.taxRateLocations, taxRate.id)
    } yield tuple).pipe(runWithTransaction)
}
