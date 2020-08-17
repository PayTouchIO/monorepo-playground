package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.DiscountRecord
import io.paytouch.core.data.model.enums.DiscountType

class DiscountsTable(tag: Tag) extends SlickMerchantTable[DiscountRecord](tag, "discounts") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def title = column[String]("title")
  def `type` = column[DiscountType]("type")
  def amount = column[BigDecimal]("amount")
  def requireManagerApproval = column[Boolean]("require_manager_approval")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      title,
      `type`,
      amount,
      requireManagerApproval,
      createdAt,
      updatedAt,
    ).<>(DiscountRecord.tupled, DiscountRecord.unapply)
}
