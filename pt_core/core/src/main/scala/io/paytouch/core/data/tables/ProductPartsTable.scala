package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.ProductPartRecord

class ProductPartsTable(tag: Tag)
    extends SlickMerchantTable[ProductPartRecord](tag, "product_parts")
       with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def productId = column[UUID]("product_id")
  def partId = column[UUID]("part_id")
  def quantityNeeded = column[BigDecimal]("quantity_needed")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      productId,
      partId,
      quantityNeeded,
      createdAt,
      updatedAt,
    ).<>(ProductPartRecord.tupled, ProductPartRecord.unapply)

}
