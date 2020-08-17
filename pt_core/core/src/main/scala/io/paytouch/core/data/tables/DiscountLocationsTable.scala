package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns }
import io.paytouch.core.data.model.DiscountLocationRecord

class DiscountLocationsTable(tag: Tag)
    extends SlickMerchantTable[DiscountLocationRecord](tag, "discount_locations")
       with ManyItemsToManyLocationsColumns
       with ActiveColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def discountId = column[UUID]("discount_id")
  def itemId = discountId // interface for ManyItemsToManyLocationsColumns
  def locationId = column[UUID]("location_id")

  def active = column[Boolean]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      discountId,
      locationId,
      active,
      createdAt,
      updatedAt,
    ).<>(DiscountLocationRecord.tupled, DiscountLocationRecord.unapply)
}
