package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns }
import io.paytouch.core.data.model.TaxRateLocationRecord

class TaxRateLocationsTable(tag: Tag)
    extends SlickMerchantTable[TaxRateLocationRecord](tag, "tax_rate_locations")
       with ManyItemsToManyLocationsColumns
       with ActiveColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def taxRateId = column[UUID]("tax_rate_id")
  def itemId = taxRateId // interface for ManyItemsToManyLocationsColumns
  def locationId = column[UUID]("location_id")

  def active = column[Boolean]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      taxRateId,
      locationId,
      active,
      createdAt,
      updatedAt,
    ).<>(TaxRateLocationRecord.tupled, TaxRateLocationRecord.unapply)
}
