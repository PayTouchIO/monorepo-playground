package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.model.OrderItemDiscountRecord
import io.paytouch.core.data.model.enums.DiscountType

class OrderItemDiscountsTable(tag: Tag)
    extends SlickMerchantTable[OrderItemDiscountRecord](tag, "order_item_discounts")
       with OrderItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderItemId = column[UUID]("order_item_id")
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
      orderItemId,
      discountId,
      title,
      `type`,
      amount,
      totalAmount,
      createdAt,
      updatedAt,
    ).<>(OrderItemDiscountRecord.tupled, OrderItemDiscountRecord.unapply)
}
