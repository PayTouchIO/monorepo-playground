package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ TaxRateLocationRecord, TaxRateLocationUpdate }
import io.paytouch.core.data.tables.TaxRateLocationsTable

class TaxRateLocationDao(
    val locationDao: LocationDao,
    taxRateDao: => TaxRateDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationToggleableDao {
  type Record = TaxRateLocationRecord
  type Update = TaxRateLocationUpdate
  type Table = TaxRateLocationsTable

  val table = TableQuery[Table]

  val itemDao = taxRateDao

  def queryByRelIds(taxRateLocationUpdate: Update) = {
    require(
      taxRateLocationUpdate.taxRateId.isDefined,
      "TaxRateLocationDao - Impossible to find by tax rate id and location id without a tax rate id",
    )

    require(
      taxRateLocationUpdate.locationId.isDefined,
      "TaxRateLocationDao - Impossible to find by tax rate id and location id without a location id",
    )

    queryFindByItemIdAndLocationId(
      taxRateLocationUpdate.taxRateId.get,
      taxRateLocationUpdate.locationId.get,
    )
  }

  def queryBulkUpsertAndDeleteTheRestByTaxRateId(taxRateLocations: Seq[Update], taxRateId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(taxRateLocations, _.taxRateId === taxRateId)

  def queryFindByLocationIdsAndTaxRateIds(locationIds: Seq[UUID], taxRateIds: Seq[UUID]) =
    table.filter(t => t.locationId.inSet(locationIds) && t.taxRateId.inSet(taxRateIds))

  def findByLocationIdsAndTaxRateIds(locationIds: Seq[UUID], taxRateIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty || taxRateIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByLocationIdsAndTaxRateIds(locationIds, taxRateIds)
        .result
        .pipe(run)
}
