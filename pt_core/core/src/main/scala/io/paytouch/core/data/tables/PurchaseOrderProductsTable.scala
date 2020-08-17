package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.PurchaseOrderProductRecord

class PurchaseOrderProductsTable(tag: Tag)
    extends SlickMerchantTable[PurchaseOrderProductRecord](tag, "purchase_order_products") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def purchaseOrderId = column[UUID]("purchase_order_id")
  def productId = column[UUID]("product_id")
  def quantity = column[Option[BigDecimal]]("quantity")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      purchaseOrderId,
      productId,
      quantity,
      costAmount,
      createdAt,
      updatedAt,
    ).<>(PurchaseOrderProductRecord.tupled, PurchaseOrderProductRecord.unapply)

}
