package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OrderDiscountRecord
import io.paytouch.core.data.model.enums.DiscountType

class OrderDiscountsTable(tag: Tag) extends SlickMerchantTable[OrderDiscountRecord](tag, "order_discounts") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderId = column[UUID]("order_id")
  def discountId = column[Option[UUID]]("discount_id")

  def title = column[Option[String]]("title")
  def `type` = column[DiscountType]("type")

  def amount = column[BigDecimal]("amount")
  def totalAmount = column[Option[BigDecimal]]("total_amount")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderId,
      discountId,
      title,
      `type`,
      amount,
      totalAmount,
      createdAt,
      updatedAt,
    ).<>(OrderDiscountRecord.tupled, OrderDiscountRecord.unapply)
}
