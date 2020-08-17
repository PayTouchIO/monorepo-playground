package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ProductLocationTaxRateRecord

class ProductLocationTaxRatesTable(tag: Tag)
    extends SlickMerchantTable[ProductLocationTaxRateRecord](tag, "product_location_tax_rates") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productLocationId = column[UUID]("product_location_id")
  def taxRateId = column[UUID]("tax_rate_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productLocationId,
      taxRateId,
      createdAt,
      updatedAt,
    ).<>(ProductLocationTaxRateRecord.tupled, ProductLocationTaxRateRecord.unapply)
}
