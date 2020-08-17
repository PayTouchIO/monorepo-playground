package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.CartTaxRateRecord
import io.paytouch.ordering.data.tables.features.{ CartIdColumn, SlickStoreTable }
import slick.lifted.Tag

class CartTaxRatesTable(tag: Tag) extends SlickStoreTable[CartTaxRateRecord](tag, "cart_tax_rates") with CartIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def storeId = column[UUID]("store_id")

  def cartId = column[UUID]("cart_id")
  def taxRateId = column[UUID]("tax_rate_id")
  def name = column[String]("name")
  def `value` = column[BigDecimal]("value")
  def totalAmount = column[BigDecimal]("total_amount")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      storeId,
      cartId,
      taxRateId,
      name,
      `value`,
      totalAmount,
      createdAt,
      updatedAt,
    ).<>(CartTaxRateRecord.tupled, CartTaxRateRecord.unapply)
}
