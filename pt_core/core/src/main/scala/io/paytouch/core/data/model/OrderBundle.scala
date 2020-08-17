package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.OrderBundleSet
import io.paytouch.core.validators.RecoveredOrderBundleSet

final case class OrderBundleRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    bundleOrderItemId: UUID,
    orderBundleSets: Seq[RecoveredOrderBundleSet],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderBundleUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
    bundleOrderItemId: Option[UUID],
    orderBundleSets: Option[Seq[RecoveredOrderBundleSet]],
  ) extends SlickMerchantUpdate[OrderBundleRecord] {

  def toRecord: OrderBundleRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderBundleUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert OrderBundleUpdate without a order id. [$this]")
    require(
      bundleOrderItemId.isDefined,
      s"Impossible to convert OrderBundleUpdate without a bundle order item id. [$this]",
    )
    OrderBundleRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      bundleOrderItemId = bundleOrderItemId.get,
      orderBundleSets = orderBundleSets.getOrElse(Seq.empty),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderBundleRecord): OrderBundleRecord =
    OrderBundleRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderId = orderId.getOrElse(record.orderId),
      bundleOrderItemId = bundleOrderItemId.getOrElse(record.bundleOrderItemId),
      orderBundleSets = orderBundleSets.getOrElse(record.orderBundleSets),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
