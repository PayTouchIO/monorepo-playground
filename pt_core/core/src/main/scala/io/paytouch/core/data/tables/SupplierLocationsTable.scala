package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns }
import io.paytouch.core.data.model.SupplierLocationRecord

class SupplierLocationsTable(tag: Tag)
    extends SlickMerchantTable[SupplierLocationRecord](tag, "supplier_locations")
       with ManyItemsToManyLocationsColumns
       with ActiveColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")
  def supplierId = column[UUID]("supplier_id")
  def itemId = supplierId // interface for ManyItemsToManyLocationsColumns

  def active = column[Boolean]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      supplierId,
      active,
      createdAt,
      updatedAt,
    ).<>(SupplierLocationRecord.tupled, SupplierLocationRecord.unapply)
}
