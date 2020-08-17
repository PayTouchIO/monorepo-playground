package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ManyItemsToManyLocationsColumns
import io.paytouch.core.data.model.LoyaltyProgramLocationRecord

class LoyaltyProgramLocationsTable(tag: Tag)
    extends SlickMerchantTable[LoyaltyProgramLocationRecord](tag, "loyalty_program_locations")
       with ManyItemsToManyLocationsColumns {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")
  def loyaltyProgramId = column[UUID]("loyalty_program_id")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def itemId = loyaltyProgramId

  def * =
    (
      id,
      merchantId,
      locationId,
      loyaltyProgramId,
      createdAt,
      updatedAt,
    ).<>(LoyaltyProgramLocationRecord.tupled, LoyaltyProgramLocationRecord.unapply)

}
