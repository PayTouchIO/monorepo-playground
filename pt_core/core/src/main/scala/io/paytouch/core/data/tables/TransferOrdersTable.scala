package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, ReceivingObjectStatusColumn }
import io.paytouch.core.data.model.TransferOrderRecord
import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, TransferOrderType }

class TransferOrdersTable(tag: Tag)
    extends SlickMerchantTable[TransferOrderRecord](tag, "transfer_orders")
       with LocationIdColumn
       with ReceivingObjectStatusColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def fromLocationId = column[UUID]("from_location_id")
  def toLocationId = column[UUID]("to_location_id")
  def userId = column[UUID]("user_id")
  def number = column[String]("number")
  def notes = column[Option[String]]("notes")
  def status = column[ReceivingObjectStatus]("status")
  def `type` = column[TransferOrderType]("type")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def locationId = fromLocationId

  def * =
    (
      id,
      merchantId,
      fromLocationId,
      toLocationId,
      userId,
      number,
      notes,
      status,
      `type`,
      createdAt,
      updatedAt,
    ).<>(TransferOrderRecord.tupled, TransferOrderRecord.unapply)

}
