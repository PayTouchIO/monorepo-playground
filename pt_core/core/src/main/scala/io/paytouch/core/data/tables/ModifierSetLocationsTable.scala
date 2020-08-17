package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ ActiveColumn, ManyItemsToManyLocationsColumns }
import io.paytouch.core.data.model.ModifierSetLocationRecord

class ModifierSetLocationsTable(tag: Tag)
    extends SlickMerchantTable[ModifierSetLocationRecord](tag, "modifier_set_locations")
       with ManyItemsToManyLocationsColumns
       with ActiveColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def modifierSetId = column[UUID]("modifier_set_id")
  def itemId = modifierSetId // interface for ManyItemsToManyLocationsColumns
  def locationId = column[UUID]("location_id")
  def active = column[Boolean]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      modifierSetId,
      locationId,
      active,
      createdAt,
      updatedAt,
    ).<>(ModifierSetLocationRecord.tupled, ModifierSetLocationRecord.unapply)
}
