package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.ProductCategoryRecord

class ProductCategoriesTable(tag: Tag)
    extends SlickMerchantTable[ProductCategoryRecord](tag, "product_categories")
       with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def categoryId = column[UUID]("category_id")
  def position = column[Int]("position")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      categoryId,
      position,
      createdAt,
      updatedAt,
    ).<>(ProductCategoryRecord.tupled, ProductCategoryRecord.unapply)
}
