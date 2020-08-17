package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ SupplierLocationRecord, SupplierLocationUpdate }
import io.paytouch.core.data.tables.SupplierLocationsTable

import scala.concurrent.ExecutionContext

class SupplierLocationDao(
    val locationDao: LocationDao,
    supplierDao: => SupplierDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationToggleableDao {

  type Record = SupplierLocationRecord
  type Update = SupplierLocationUpdate
  type Table = SupplierLocationsTable

  val table = TableQuery[Table]

  val itemDao = supplierDao

  implicit val l: LocationDao = locationDao

  def queryByRelIds(supplierLocationUpdate: Update) = {
    require(
      supplierLocationUpdate.supplierId.isDefined,
      "SupplierLocationDao - Impossible to find by supplier id and location id without a supplier id",
    )
    require(
      supplierLocationUpdate.locationId.isDefined,
      "SupplierLocationDao - Impossible to find by supplier id and location id without a location id",
    )
    queryFindByItemIdAndLocationId(supplierLocationUpdate.supplierId.get, supplierLocationUpdate.locationId.get)
  }

  def queryFindBySupplierIdAndLocationIds(supplierId: UUID, locationIds: Seq[UUID]) =
    table.filter(_.supplierId === supplierId).filterByLocationIds(locationIds)
}
