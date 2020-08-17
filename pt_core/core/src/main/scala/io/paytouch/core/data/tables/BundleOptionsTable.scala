package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.BundleOptionRecord

class BundleOptionsTable(tag: Tag) extends SlickMerchantTable[BundleOptionRecord](tag, "bundle_options") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def bundleSetId = column[UUID]("bundle_set_id")
  def articleId = column[UUID]("article_id")
  def priceAdjustment = column[BigDecimal]("price_adjustment")
  def position = column[Int]("position")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      bundleSetId,
      articleId,
      priceAdjustment,
      position,
      createdAt,
      updatedAt,
    ).<>(BundleOptionRecord.tupled, BundleOptionRecord.unapply)
}
