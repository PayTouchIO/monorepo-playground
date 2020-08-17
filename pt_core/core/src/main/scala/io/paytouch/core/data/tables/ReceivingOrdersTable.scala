package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.LocationIdColumn
import io.paytouch.core.data.model.ReceivingOrderRecord
import io.paytouch.core.data.model.enums.{
  ReceivingOrderObjectType,
  ReceivingOrderPaymentMethod,
  ReceivingOrderPaymentStatus,
  ReceivingOrderStatus,
}

class ReceivingOrdersTable(tag: Tag)
    extends SlickMerchantTable[ReceivingOrderRecord](tag, "receiving_orders")
       with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")
  def userId = column[UUID]("user_id")
  def receivingObjectType = column[Option[ReceivingOrderObjectType]]("receiving_object_type")
  def receivingObjectId = column[Option[UUID]]("receiving_object_id")
  def status = column[ReceivingOrderStatus]("status")
  def number = column[String]("number")
  def synced = column[Boolean]("synced")
  def invoiceNumber = column[Option[String]]("invoice_number")
  def paymentMethod = column[Option[ReceivingOrderPaymentMethod]]("payment_method")
  def paymentStatus = column[Option[ReceivingOrderPaymentStatus]]("payment_status")
  def paymentDueDate = column[Option[ZonedDateTime]]("payment_due_date")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      userId,
      receivingObjectType,
      receivingObjectId,
      status,
      number,
      synced,
      invoiceNumber,
      paymentMethod,
      paymentStatus,
      paymentDueDate,
      createdAt,
      updatedAt,
    ).<>(ReceivingOrderRecord.tupled, ReceivingOrderRecord.unapply)

}
