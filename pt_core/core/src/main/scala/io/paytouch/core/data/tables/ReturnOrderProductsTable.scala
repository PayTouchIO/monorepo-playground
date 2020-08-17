package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ReturnOrderProductRecord
import io.paytouch.core.data.model.enums.{ ReturnOrderReason, UnitType }

class ReturnOrderProductsTable(tag: Tag)
    extends SlickMerchantTable[ReturnOrderProductRecord](tag, "return_order_products") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def returnOrderId = column[UUID]("return_order_id")
  def productId = column[UUID]("product_id")
  def productName = column[String]("product_name")
  def productUnit = column[UnitType]("product_unit")
  def quantity = column[Option[BigDecimal]]("quantity")
  def reason = column[ReturnOrderReason]("reason")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      returnOrderId,
      productId,
      productName,
      productUnit,
      quantity,
      reason,
      createdAt,
      updatedAt,
    ).<>(ReturnOrderProductRecord.tupled, ReturnOrderProductRecord.unapply)

}
