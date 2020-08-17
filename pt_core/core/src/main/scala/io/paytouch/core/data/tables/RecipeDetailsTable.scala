package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.RecipeDetailRecord

class RecipeDetailsTable(tag: Tag)
    extends SlickMerchantTable[RecipeDetailRecord](tag, "recipe_details")
       with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def makesQuantity = column[BigDecimal]("makes_quantity", O.Default(0.0))
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      makesQuantity,
      createdAt,
      updatedAt,
    ).<>(RecipeDetailRecord.tupled, RecipeDetailRecord.unapply)

}
