package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.VariantOptionRecord

class VariantOptionsTable(tag: Tag)
    extends SlickMerchantTable[VariantOptionRecord](tag, "variant_options")
       with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def variantOptionTypeId = column[UUID]("variant_option_type_id")
  def name = column[String]("name")
  def position = column[Int]("position")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      variantOptionTypeId,
      name,
      position,
      createdAt,
      updatedAt,
    ).<>(VariantOptionRecord.tupled, VariantOptionRecord.unapply)
}
