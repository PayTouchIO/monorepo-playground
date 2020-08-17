package io.paytouch.ordering.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.CartItemModifierOptionRecord
import io.paytouch.ordering.data.tables.features.{ CartItemIdColumn, SlickStoreTable }
import io.paytouch.ordering.entities.enums.ModifierSetType
import slick.lifted.Tag

class CartItemModifierOptionsTable(tag: Tag)
    extends SlickStoreTable[CartItemModifierOptionRecord](tag, "cart_item_modifier_options")
       with CartItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def storeId = column[UUID]("store_id")

  def cartItemId = column[UUID]("cart_item_id")
  def modifierOptionId = column[UUID]("modifier_option_id")
  def name = column[String]("name")
  def `type` = column[ModifierSetType]("type")
  def priceAmount = column[BigDecimal]("price_amount")
  def quantity = column[BigDecimal]("quantity")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def cartItemRelId = modifierOptionId

  def * =
    (
      id,
      storeId,
      cartItemId,
      modifierOptionId,
      name,
      `type`,
      priceAmount,
      quantity,
      createdAt,
      updatedAt,
    ).<>(CartItemModifierOptionRecord.tupled, CartItemModifierOptionRecord.unapply)
}
