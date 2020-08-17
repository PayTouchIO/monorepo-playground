package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.LocationIdColumn
import io.paytouch.core.data.model.ReturnOrderRecord
import io.paytouch.core.data.model.enums.ReturnOrderStatus

class ReturnOrdersTable(tag: Tag)
    extends SlickMerchantTable[ReturnOrderRecord](tag, "return_orders")
       with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")
  def supplierId = column[UUID]("supplier_id")
  def locationId = column[UUID]("location_id")
  def purchaseOrderId = column[Option[UUID]]("purchase_order_id")
  def number = column[String]("number")
  def notes = column[Option[String]]("notes")
  def status = column[ReturnOrderStatus]("status")
  def synced = column[Boolean]("synced")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      userId,
      supplierId,
      locationId,
      purchaseOrderId,
      number,
      notes,
      status,
      synced,
      createdAt,
      updatedAt,
    ).<>(ReturnOrderRecord.tupled, ReturnOrderRecord.unapply)

}
