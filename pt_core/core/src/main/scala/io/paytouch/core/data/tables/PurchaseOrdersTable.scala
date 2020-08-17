package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, ReceivingObjectStatusColumn }
import io.paytouch.core.data.model.PurchaseOrderRecord
import io.paytouch.core.data.model.enums.{ PurchaseOrderPaymentStatus, ReceivingObjectStatus }

class PurchaseOrdersTable(tag: Tag)
    extends SlickSoftDeleteTable[PurchaseOrderRecord](tag, "purchase_orders")
       with LocationIdColumn
       with ReceivingObjectStatusColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def supplierId = column[UUID]("supplier_id")
  def locationId = column[UUID]("location_id")
  def userId = column[UUID]("user_id")
  def number = column[String]("number")
  def sent = column[Boolean]("sent")
  def paymentStatus = column[Option[PurchaseOrderPaymentStatus]]("payment_status")
  def expectedDeliveryDate = column[Option[ZonedDateTime]]("expected_delivery_date")
  def status = column[ReceivingObjectStatus]("status")
  def notes = column[Option[String]]("notes")
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      supplierId,
      locationId,
      userId,
      number,
      sent,
      paymentStatus,
      expectedDeliveryDate,
      status,
      notes,
      deletedAt,
      createdAt,
      updatedAt,
    ).<>(PurchaseOrderRecord.tupled, PurchaseOrderRecord.unapply)

}
