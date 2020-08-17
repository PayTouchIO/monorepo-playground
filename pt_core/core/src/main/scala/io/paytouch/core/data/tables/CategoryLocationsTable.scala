package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns }
import io.paytouch.core.data.model.CategoryLocationRecord

class CategoryLocationsTable(tag: Tag)
    extends SlickMerchantTable[CategoryLocationRecord](tag, "category_locations")
       with ManyItemsToManyLocationsColumns
       with ActiveColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")
  def categoryId = column[UUID]("category_id")
  def itemId = categoryId

  def active = column[Boolean]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      categoryId,
      active,
      createdAt,
      updatedAt,
    ).<>(CategoryLocationRecord.tupled, CategoryLocationRecord.unapply)
}
