package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.GiftCardPassTransactionRecord

class GiftCardPassTransactionsTable(tag: Tag)
    extends SlickMerchantTable[GiftCardPassTransactionRecord](tag, "gift_card_pass_transactions") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def giftCardPassId = column[UUID]("gift_card_pass_id")
  def totalAmount = column[BigDecimal]("total_amount")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      giftCardPassId,
      totalAmount,
      createdAt,
      updatedAt,
    ).<>(GiftCardPassTransactionRecord.tupled, GiftCardPassTransactionRecord.unapply)
}
