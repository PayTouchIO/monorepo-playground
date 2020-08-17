package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.CartItemTaxRateRecord
import io.paytouch.ordering.data.tables.features.{ CartItemIdColumn, SlickStoreTable }
import slick.lifted.Tag

class CartItemTaxRatesTable(tag: Tag)
    extends SlickStoreTable[CartItemTaxRateRecord](tag, "cart_item_tax_rates")
       with CartItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def storeId = column[UUID]("store_id")

  def cartItemId = column[UUID]("cart_item_id")
  def taxRateId = column[UUID]("tax_rate_id")
  def name = column[String]("name")
  def value = column[BigDecimal]("value")
  def totalAmount = column[BigDecimal]("total_amount")
  def applyToPrice = column[Boolean]("apply_to_price")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def cartItemRelId = taxRateId

  def * =
    (
      id,
      storeId,
      cartItemId,
      taxRateId,
      name,
      value,
      totalAmount,
      applyToPrice,
      createdAt,
      updatedAt,
    ).<>(CartItemTaxRateRecord.tupled, CartItemTaxRateRecord.unapply)
}
