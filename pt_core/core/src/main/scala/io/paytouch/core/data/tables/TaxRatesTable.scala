package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.TaxRateRecord

class TaxRatesTable(tag: Tag) extends SlickMerchantTable[TaxRateRecord](tag, "tax_rates") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def name = column[String]("name")
  def value = column[BigDecimal]("value")
  def applyToPrice = column[Boolean]("apply_to_price")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id, merchantId, name, value, applyToPrice, createdAt, updatedAt).<>(TaxRateRecord.tupled, TaxRateRecord.unapply)
}
