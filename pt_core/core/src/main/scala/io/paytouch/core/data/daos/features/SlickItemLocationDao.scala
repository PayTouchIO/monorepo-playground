package io.paytouch.core.data.daos.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos._
import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ManyItemsToManyLocationsColumns
import io.paytouch.core.data.tables.SlickMerchantTable

trait SlickItemLocationDao extends SlickRelDao {
  type Table <: SlickMerchantTable[Record] with ManyItemsToManyLocationsColumns

  implicit def locationDao: LocationDao

  def queryFindByLocationId(locationId: UUID) = queryFindByLocationIds(Seq(locationId))

  def queryFindByLocationIds(locationIds: Seq[UUID]) = baseQuery.filterByLocationIds(locationIds)

  def queryFindByItemId(itemId: UUID) = queryFindByItemIds(Seq(itemId))

  def queryFindByItemIds(itemIds: Seq[UUID]) =
    baseQuery
      .filterByItemIds(itemIds)
      .join(locationDao.nonDeletedTable)
      .on(_.locationId === _.id)
      .map {
        case (thisTable, _) => thisTable
      }

  def queryFindByItemIdAndLocationId(itemId: UUID, locationId: UUID) =
    queryFindByItemIdsAndLocationIds(Seq(itemId), Seq(locationId))

  def queryFindByItemIdsAndLocationIds(itemIds: Seq[UUID], locationIds: Seq[UUID]) =
    baseQuery
      .filterByItemIds(itemIds)
      .filterByLocationIds(locationIds)

  def findByItemIds(itemIds: Seq[UUID]): Future[Seq[Record]] =
    if (itemIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByItemIds(itemIds)
        .result
        .pipe(run)

  def findByItemId(itemId: UUID): Future[Seq[Record]] =
    findByItemIds(Seq(itemId))

  def findByLocationIds(locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByLocationIds(locationIds)
        .result
        .pipe(run)

  def findByLocationId(locationId: UUID): Future[Seq[Record]] =
    findByLocationIds(Seq(locationId))

  def findByItemIdsAndLocationIds(itemIds: Seq[UUID], locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (itemIds.isEmpty || locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByItemIdsAndLocationIds(itemIds, locationIds)
        .result
        .pipe(run)

  def findByItemIdAndLocationIds(itemId: UUID, locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByItemIdsAndLocationIds(Seq(itemId), locationIds)
        .result
        .pipe(run)

  def findOneByItemIdAndLocationId(itemId: UUID, locationId: UUID): Future[Option[Record]] =
    queryFindByItemIdAndLocationId(itemId, locationId)
      .result
      .headOption
      .pipe(run)

  def queryBulkUpsertAndDeleteTheRest(itemLocations: Map[UUID, Option[Update]], itemId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(
      itemLocations.values.flatten.toSeq,
      t => (t.itemId === itemId) && (t.locationId inSet itemLocations.keys),
    )
}
