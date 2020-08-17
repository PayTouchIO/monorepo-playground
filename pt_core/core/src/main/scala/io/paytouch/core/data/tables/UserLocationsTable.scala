package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ManyItemsToManyLocationsColumns
import io.paytouch.core.data.model.UserLocationRecord

class UserLocationsTable(tag: Tag)
    extends SlickMerchantTable[UserLocationRecord](tag, "user_locations")
       with ManyItemsToManyLocationsColumns {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def locationId = column[UUID]("location_id")
  def userId = column[UUID]("user_id")

  def itemId = userId

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      userId,
      createdAt,
      updatedAt,
    ).<>(UserLocationRecord.tupled, UserLocationRecord.unapply)
}
