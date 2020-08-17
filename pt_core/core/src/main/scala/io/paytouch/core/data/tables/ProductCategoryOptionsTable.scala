package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ProductCategoryOptionRecord

class ProductCategoryOptionsTable(tag: Tag)
    extends SlickMerchantTable[ProductCategoryOptionRecord](tag, "product_category_options") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productCategoryId = column[UUID]("product_category_id")
  def deliveryEnabled = column[Boolean]("delivery_enabled")
  def takeAwayEnabled = column[Boolean]("take_away_enabled")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productCategoryId,
      deliveryEnabled,
      takeAwayEnabled,
      createdAt,
      updatedAt,
    ).<>(ProductCategoryOptionRecord.tupled, ProductCategoryOptionRecord.unapply)
}
