package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.ProductVariantOptionRecord

class ProductVariantOptionsTable(tag: Tag)
    extends SlickMerchantTable[ProductVariantOptionRecord](tag, "product_variant_options")
       with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def variantOptionId = column[UUID]("variant_option_id")
  def productId = column[UUID]("product_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      variantOptionId,
      productId,
      createdAt,
      updatedAt,
    ).<>(ProductVariantOptionRecord.tupled, ProductVariantOptionRecord.unapply)
}
