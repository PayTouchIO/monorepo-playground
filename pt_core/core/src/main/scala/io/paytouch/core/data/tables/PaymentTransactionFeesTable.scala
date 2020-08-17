package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.PaymentTransactionFeeRecord
import io.paytouch.core.data.model.enums.PaymentTransactionFeeType

class PaymentTransactionFeesTable(tag: Tag)
    extends SlickMerchantTable[PaymentTransactionFeeRecord](tag, "payment_transaction_fees") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def paymentTransactionId = column[UUID]("payment_transaction_id")
  def name = column[String]("name")
  def `type` = column[PaymentTransactionFeeType]("type")
  def amount = column[BigDecimal]("amount")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      paymentTransactionId,
      name,
      `type`,
      amount,
      createdAt,
      updatedAt,
    ).<>(PaymentTransactionFeeRecord.tupled, PaymentTransactionFeeRecord.unapply)
}
