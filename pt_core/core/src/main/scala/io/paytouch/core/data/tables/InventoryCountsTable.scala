package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.LocationIdColumn
import io.paytouch.core.data.model.InventoryCountRecord
import io.paytouch.core.data.model.enums.InventoryCountStatus

class InventoryCountsTable(tag: Tag)
    extends SlickMerchantTable[InventoryCountRecord](tag, "inventory_counts")
       with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")
  def locationId = column[UUID]("location_id")
  def number = column[String]("number")
  def valueChangeAmount = column[Option[BigDecimal]]("value_change_amount")
  def status = column[InventoryCountStatus]("status")
  def synced = column[Boolean]("synced", O.Default(false))
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      userId,
      locationId,
      number,
      valueChangeAmount,
      status,
      synced,
      createdAt,
      updatedAt,
    ).<>(InventoryCountRecord.tupled, InventoryCountRecord.unapply)

}
