package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.TransferOrderProductRecord
import io.paytouch.core.data.model.enums.UnitType

class TransferOrderProductsTable(tag: Tag)
    extends SlickMerchantTable[TransferOrderProductRecord](tag, "transfer_order_products") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def transferOrderId = column[UUID]("transfer_order_id")
  def productId = column[UUID]("product_id")
  def productName = column[String]("product_name")
  def productUnit = column[UnitType]("product_unit")
  def quantity = column[Option[BigDecimal]]("quantity")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      transferOrderId,
      productId,
      productName,
      productUnit,
      quantity,
      createdAt,
      updatedAt,
    ).<>(TransferOrderProductRecord.tupled, TransferOrderProductRecord.unapply)

}
