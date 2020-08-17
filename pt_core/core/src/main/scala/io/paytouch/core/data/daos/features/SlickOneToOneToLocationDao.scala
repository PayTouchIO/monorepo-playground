package io.paytouch.core.data.daos.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos._
import io.paytouch.core.data.daos.LocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OneToOneLocationColumns
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.SlickMerchantTable

trait SlickOneToOneToLocationDao extends SlickMerchantDao {
  type Record <: SlickOneToOneWithLocationRecord
  type Table <: SlickMerchantTable[Record] with OneToOneLocationColumns

  implicit def locationDao: LocationDao

  def queryFindOneByLocationId(locationId: UUID): Query[Table, Record, Seq] =
    baseQuery.filterByLocationId(locationId)

  def queryFindOneByMerchantIdAndLocationId(merchantId: UUID, locationId: UUID): Query[Table, Record, Seq] =
    baseQuery
      .filterByLocationId(locationId)
      .filter(_.merchantId === merchantId)

  override def queryUpsert(entityUpdate: Update) = {
    val record = entityUpdate.toRecord // toRecord should not be used just to get out merchantId and locationId

    queryUpsertByQuery(entityUpdate, queryFindOneByMerchantIdAndLocationId(record.merchantId, record.locationId))
  }

  def findByLocationIds(locationIds: Seq[UUID], merchantId: UUID): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      baseQuery
        .filterByLocationIds(locationIds)
        .filter(_.merchantId === merchantId)
        .result
        .pipe(run)

  def findByLocationId(locationId: UUID, merchantId: UUID): Future[Option[Record]] =
    queryFindOneByMerchantIdAndLocationId(merchantId, locationId)
      .result
      .headOption
      .pipe(run)
}
