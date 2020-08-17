package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.InventoryCountProductRecord

class InventoryCountProductsTable(tag: Tag)
    extends SlickMerchantTable[InventoryCountProductRecord](tag, "inventory_count_products") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def inventoryCountId = column[UUID]("inventory_count_id")
  def productId = column[UUID]("product_id")
  def productName = column[String]("product_name")
  def expectedQuantity = column[Option[BigDecimal]]("expected_quantity", O.Default(Some(0.0)))
  def countedQuantity = column[Option[BigDecimal]]("counted_quantity", O.Default(Some(0.0)))
  def valueAmount = column[Option[BigDecimal]]("value_amount")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def valueChangeAmount = column[Option[BigDecimal]]("value_change_amount")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      inventoryCountId,
      productId,
      productName,
      expectedQuantity,
      countedQuantity,
      valueAmount,
      costAmount,
      valueChangeAmount,
      createdAt,
      updatedAt,
    ).<>(InventoryCountProductRecord.tupled, InventoryCountProductRecord.unapply)

}
