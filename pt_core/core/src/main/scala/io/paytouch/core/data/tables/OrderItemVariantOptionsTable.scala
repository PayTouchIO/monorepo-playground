package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.model.OrderItemVariantOptionRecord

class OrderItemVariantOptionsTable(tag: Tag)
    extends SlickMerchantTable[OrderItemVariantOptionRecord](tag, "order_item_variant_options")
       with OrderItemIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def orderItemId = column[UUID]("order_item_id")
  def variantOptionId = column[Option[UUID]]("variant_option_id")
  def optionName = column[String]("option_name")
  def optionTypeName = column[String]("option_type_name")
  def position = column[Int]("position")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderItemId,
      variantOptionId,
      optionName,
      optionTypeName,
      position,
      createdAt,
      updatedAt,
    ).<>(OrderItemVariantOptionRecord.tupled, OrderItemVariantOptionRecord.unapply)
}
