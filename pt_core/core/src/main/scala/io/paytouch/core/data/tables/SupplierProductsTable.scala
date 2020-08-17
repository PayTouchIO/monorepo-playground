package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SupplierProductRecord

class SupplierProductsTable(tag: Tag) extends SlickMerchantTable[SupplierProductRecord](tag, "supplier_products") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def supplierId = column[UUID]("supplier_id")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      supplierId,
      createdAt,
      updatedAt,
    ).<>(SupplierProductRecord.tupled, SupplierProductRecord.unapply)
}
