package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ KitchenRecord, KitchenUpdate }
import io.paytouch.core.data.tables.KitchensTable
import io.paytouch.core.filters.KitchenFilters

class KitchenDao(val locationDao: LocationDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickSoftDeleteDao
       with SlickDefaultUpsertDao {
  type Record = KitchenRecord
  type Update = KitchenUpdate
  type Table = KitchensTable
  type Filters = KitchenFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = filters.locationIds,
      updatedSince = filters.updatedSince,
    ).drop(offset).take(limit).result.pipe(run)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = filters.locationIds,
      updatedSince = filters.updatedSince,
    ).length.result.pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      updatedSince: Option[ZonedDateTime],
    ) =
    nonDeletedTable
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          Some(t.locationId in locationDao.queryFindByIds(locationIds).map(_.id)),
          updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
        ),
      )
      .sortBy(_.name)

  def findByMerchantAndLocationIds(merchantId: UUID, locationIds: Seq[UUID]) =
    queryFindAllByMerchantId(merchantId, locationIds, None)
      .result
      .pipe(run)

  def findByNamesAndMerchantId(
      names: Seq[String],
      merchantId: UUID,
      locationId: UUID,
    ): Future[Seq[Record]] =
    if (names.isEmpty)
      Future.successful(Seq.empty)
    else
      nonDeletedTable
        .filter(_.name.toLowerCase.inSet(names.map(_.toLowerCase)))
        .filter(_.merchantId === merchantId)
        .filter(_.locationId === locationId)
        .result
        .pipe(run)

  implicit val l: LocationDao = locationDao

  def findByLocationIds(locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByLocationIds(locationIds)
        .result
        .pipe(run)

  def findByLocationId(locationId: UUID): Future[Seq[Record]] =
    findByLocationIds(Seq(locationId))

  def queryFindByLocationId(locationId: UUID) =
    queryFindByLocationIds(Seq(locationId))

  def queryFindByLocationIds(locationIds: Seq[UUID]) =
    nonDeletedTable.filterByLocationIds(locationIds)
}
