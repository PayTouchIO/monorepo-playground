package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.model.OrderItemTaxRateRecord

class OrderItemTaxRatesTable(tag: Tag)
    extends SlickMerchantTable[OrderItemTaxRateRecord](tag, "order_item_tax_rates")
       with OrderItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderItemId = column[UUID]("order_item_id")
  def taxRateId = column[Option[UUID]]("tax_rate_id")
  def name = column[String]("name")
  def value = column[BigDecimal]("value")
  def totalAmount = column[Option[BigDecimal]]("total_amount")
  def applyToPrice = column[Boolean]("apply_to_price", O.Default(false))
  def active = column[Boolean]("active", O.Default(true))
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderItemId,
      taxRateId,
      name,
      value,
      totalAmount,
      applyToPrice,
      active,
      createdAt,
      updatedAt,
    ).<>(OrderItemTaxRateRecord.tupled, OrderItemTaxRateRecord.unapply)

}
