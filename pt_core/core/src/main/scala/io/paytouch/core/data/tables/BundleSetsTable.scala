package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ProductIdColumn
import io.paytouch.core.data.model.BundleSetRecord

class BundleSetsTable(tag: Tag) extends SlickMerchantTable[BundleSetRecord](tag, "bundle_sets") with ProductIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def bundleId = column[UUID]("bundle_id")
  def name = column[Option[String]]("name")
  def position = column[Int]("position")
  def minQuantity = column[Int]("min_quantity")
  def maxQuantity = column[Int]("max_quantity")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def productId = bundleId

  def * =
    (
      id,
      merchantId,
      bundleId,
      name,
      position,
      minQuantity,
      maxQuantity,
      createdAt,
      updatedAt,
    ).<>(BundleSetRecord.tupled, BundleSetRecord.unapply)
}
