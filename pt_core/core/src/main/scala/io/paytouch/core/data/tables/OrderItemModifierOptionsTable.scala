package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.model.OrderItemModifierOptionRecord
import io.paytouch.core.data.model.enums.ModifierSetType

class OrderItemModifierOptionsTable(tag: Tag)
    extends SlickMerchantTable[OrderItemModifierOptionRecord](tag, "order_item_modifier_options")
       with OrderItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderItemId = column[UUID]("order_item_id")
  def modifierOptionId = column[Option[UUID]]("modifier_option_id")

  def name = column[String]("name")
  def modifierSetName = column[Option[String]]("modifier_set_name")
  def `type` = column[ModifierSetType]("type")

  def priceAmount = column[BigDecimal]("price_amount")
  def quantity = column[BigDecimal]("quantity")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderItemId,
      modifierOptionId,
      name,
      modifierSetName,
      `type`,
      priceAmount,
      quantity,
      createdAt,
      updatedAt,
    ).<>(OrderItemModifierOptionRecord.tupled, OrderItemModifierOptionRecord.unapply)
}
