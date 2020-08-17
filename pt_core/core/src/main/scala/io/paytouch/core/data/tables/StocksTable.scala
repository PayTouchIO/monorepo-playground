package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions._
import io.paytouch.core.data.model.StockRecord

class StocksTable(tag: Tag)
    extends SlickMerchantTable[StockRecord](tag, "stocks")
       with LocationIdColumn
       with ProductIdColumn
       with OneToOneLocationColumns {
  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def productId = column[UUID]("product_id")
  def locationId = column[UUID]("location_id")

  def quantity = column[BigDecimal]("quantity")
  def minimumOnHand = column[BigDecimal]("minimum_on_hand")
  def reorderAmount = column[BigDecimal]("reorder_amount")
  def sellOutOfStock = column[Boolean]("sell_out_of_stock")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      locationId,
      quantity,
      minimumOnHand,
      reorderAmount,
      sellOutOfStock,
      createdAt,
      updatedAt,
    ).<>(StockRecord.tupled, StockRecord.unapply)
}
