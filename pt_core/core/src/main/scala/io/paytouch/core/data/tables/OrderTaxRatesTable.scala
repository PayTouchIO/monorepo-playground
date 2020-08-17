package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OrderTaxRateRecord

class OrderTaxRatesTable(tag: Tag) extends SlickMerchantTable[OrderTaxRateRecord](tag, "order_tax_rates") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderId = column[UUID]("order_id")
  def taxRateId = column[Option[UUID]]("tax_rate_id")
  def name = column[String]("name")
  def value = column[BigDecimal]("value")
  def totalAmount = column[BigDecimal]("total_amount")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderId,
      taxRateId,
      name,
      value,
      totalAmount,
      createdAt,
      updatedAt,
    ).<>(OrderTaxRateRecord.tupled, OrderTaxRateRecord.unapply)

}
