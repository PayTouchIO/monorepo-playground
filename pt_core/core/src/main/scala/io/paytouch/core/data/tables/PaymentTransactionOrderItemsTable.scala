package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.model.PaymentTransactionOrderItemRecord

class PaymentTransactionOrderItemsTable(tag: Tag)
    extends SlickMerchantTable[PaymentTransactionOrderItemRecord](tag, "payment_transaction_order_items")
       with OrderItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def paymentTransactionId = column[UUID]("payment_transaction_id")
  def orderItemId = column[UUID]("order_item_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      paymentTransactionId,
      orderItemId,
      createdAt,
      updatedAt,
    ).<>(PaymentTransactionOrderItemRecord.tupled, PaymentTransactionOrderItemRecord.unapply)
}
