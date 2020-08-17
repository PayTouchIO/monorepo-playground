package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.PaymentTransactionRecord
import io.paytouch.core.data.model.enums.{ TransactionPaymentProcessor, TransactionPaymentType, TransactionType }
import io.paytouch.core.entities.PaymentDetails
import io.paytouch.core.json.JsonSupport.JValue

class PaymentTransactionsTable(tag: Tag)
    extends SlickMerchantTable[PaymentTransactionRecord](tag, "payment_transactions") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def orderId = column[UUID]("order_id")
  def customerId = column[Option[UUID]]("customer_id")

  def `type` = column[Option[TransactionType]]("type")
  def refundedPaymentTransactionId = column[Option[UUID]]("refunded_payment_transaction_id")
  def paymentType = column[Option[TransactionPaymentType]]("payment_type")
  def paymentDetails = column[Option[PaymentDetails]]("payment_details")
  def paymentDetailsJson = column[Option[JValue]]("payment_details")
  def paymentProcessor = column[TransactionPaymentProcessor]("payment_processor")

  def version = column[Int]("version")

  def paidAt = column[Option[ZonedDateTime]]("paid_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderId,
      customerId,
      `type`,
      refundedPaymentTransactionId,
      paymentType,
      paymentDetails,
      version,
      paidAt,
      paymentProcessor,
      createdAt,
      updatedAt,
    ).<>(PaymentTransactionRecord.tupled, PaymentTransactionRecord.unapply)
}
