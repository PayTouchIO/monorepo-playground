package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OrderBundleRecord
import io.paytouch.core.validators.RecoveredOrderBundleSet

class OrderBundlesTable(tag: Tag) extends SlickMerchantTable[OrderBundleRecord](tag, "order_bundles") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def orderId = column[UUID]("order_id")
  def bundleOrderItemId = column[UUID]("bundle_order_item_id")
  def orderBundleSets = column[Seq[RecoveredOrderBundleSet]]("order_bundle_sets")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      orderId,
      bundleOrderItemId,
      orderBundleSets,
      createdAt,
      updatedAt,
    ).<>(OrderBundleRecord.tupled, OrderBundleRecord.unapply)
}
