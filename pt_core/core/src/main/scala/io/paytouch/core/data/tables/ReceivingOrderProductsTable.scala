package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ReceivingOrderProductRecord
import io.paytouch.core.data.model.enums.UnitType

class ReceivingOrderProductsTable(tag: Tag)
    extends SlickMerchantTable[ReceivingOrderProductRecord](tag, "receiving_order_products") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def receivingOrderId = column[UUID]("receiving_order_id")
  def productId = column[UUID]("product_id")
  def productName = column[String]("product_name")
  def productUnit = column[UnitType]("product_unit")
  def quantity = column[Option[BigDecimal]]("quantity")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      receivingOrderId,
      productId,
      productName,
      productUnit,
      quantity,
      costAmount,
      createdAt,
      updatedAt,
    ).<>(ReceivingOrderProductRecord.tupled, ReceivingOrderProductRecord.unapply)

}
