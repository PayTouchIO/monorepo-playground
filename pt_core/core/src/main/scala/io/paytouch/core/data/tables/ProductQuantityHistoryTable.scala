package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, ProductHistoryColumns }
import io.paytouch.core.data.model.ProductQuantityHistoryRecord
import io.paytouch.core.data.model.enums.QuantityChangeReason

class ProductQuantityHistoryTable(tag: Tag)
    extends SlickMerchantTable[ProductQuantityHistoryRecord](tag, "product_quantity_history")
       with ProductHistoryColumns
       with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def locationId = column[UUID]("location_id")
  def userId = column[Option[UUID]]("user_id")
  def orderId = column[Option[UUID]]("order_id")

  def date = column[ZonedDateTime]("date")
  def prevQuantityAmount = column[BigDecimal]("prev_quantity_amount")
  def newQuantityAmount = column[BigDecimal]("new_quantity_amount")
  def newStockValueAmount = column[BigDecimal]("new_stock_value_amount")
  def reason = column[QuantityChangeReason]("reason")
  def notes = column[Option[String]]("notes")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      locationId,
      userId,
      orderId,
      date,
      prevQuantityAmount,
      newQuantityAmount,
      newStockValueAmount,
      reason,
      notes,
      createdAt,
      updatedAt,
    ).<>(ProductQuantityHistoryRecord.tupled, ProductQuantityHistoryRecord.unapply)
}
