package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.{ LocationIdColumn, ProductHistoryColumns }
import io.paytouch.core.data.model.ProductPriceHistoryRecord
import io.paytouch.core.data.model.enums.ChangeReason

class ProductPriceHistoryTable(tag: Tag)
    extends SlickMerchantTable[ProductPriceHistoryRecord](tag, "product_price_history")
       with ProductHistoryColumns
       with LocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def locationId = column[UUID]("location_id")
  def userId = column[UUID]("user_id")

  def date = column[ZonedDateTime]("date")
  def prevPriceAmount = column[BigDecimal]("prev_price_amount")
  def newPriceAmount = column[BigDecimal]("new_price_amount")
  def reason = column[ChangeReason]("reason")
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
      date,
      prevPriceAmount,
      newPriceAmount,
      reason,
      notes,
      createdAt,
      updatedAt,
    ).<>(ProductPriceHistoryRecord.tupled, ProductPriceHistoryRecord.unapply)
}
