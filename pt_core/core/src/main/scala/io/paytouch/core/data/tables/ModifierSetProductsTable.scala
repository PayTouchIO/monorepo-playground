package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.ModifierSetProductRecord

class ModifierSetProductsTable(tag: Tag)
    extends SlickMerchantTable[ModifierSetProductRecord](tag, "modifier_set_products")
       with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def modifierSetId = column[UUID]("modifier_set_id")
  def productId = column[UUID]("product_id")
  def position = column[Option[Int]]("position")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      modifierSetId,
      productId,
      position,
      createdAt,
      updatedAt,
    ).<>(ModifierSetProductRecord.tupled, ModifierSetProductRecord.unapply)
}
