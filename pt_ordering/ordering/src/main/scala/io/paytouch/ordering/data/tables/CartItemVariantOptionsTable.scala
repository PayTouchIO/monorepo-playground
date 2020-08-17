package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.CartItemVariantOptionRecord
import io.paytouch.ordering.data.tables.features.{ CartItemIdColumn, SlickStoreTable }
import slick.lifted.Tag

class CartItemVariantOptionsTable(tag: Tag)
    extends SlickStoreTable[CartItemVariantOptionRecord](tag, "cart_item_variant_options")
       with CartItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def storeId = column[UUID]("store_id")

  def cartItemId = column[UUID]("cart_item_id")
  def variantOptionId = column[UUID]("variant_option_id")
  def optionName = column[String]("option_name")
  def optionTypeName = column[String]("option_type_name")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def cartItemRelId = variantOptionId

  def * =
    (
      id,
      storeId,
      cartItemId,
      variantOptionId,
      optionName,
      optionTypeName,
      createdAt,
      updatedAt,
    ).<>(CartItemVariantOptionRecord.tupled, CartItemVariantOptionRecord.unapply)
}
