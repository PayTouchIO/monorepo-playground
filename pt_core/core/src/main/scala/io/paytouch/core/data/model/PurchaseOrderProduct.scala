package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

final case class PurchaseOrderProductRecord(
    id: UUID,
    merchantId: UUID,
    purchaseOrderId: UUID,
    productId: UUID,
    quantity: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class PurchaseOrderProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    purchaseOrderId: Option[UUID],
    productId: Option[UUID],
    quantity: Option[BigDecimal],
    costAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[PurchaseOrderProductRecord] {

  def toRecord: PurchaseOrderProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert PurchaseOrderProductUpdate without a merchant id. [$this]")
    require(
      purchaseOrderId.isDefined,
      s"Impossible to convert PurchaseOrderProductUpdate without a purchase order id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert PurchaseOrderProductUpdate without a product id. [$this]")
    PurchaseOrderProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      purchaseOrderId = purchaseOrderId.get,
      productId = productId.get,
      quantity = quantity,
      costAmount = costAmount,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PurchaseOrderProductRecord): PurchaseOrderProductRecord =
    PurchaseOrderProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      purchaseOrderId = purchaseOrderId.getOrElse(record.purchaseOrderId),
      productId = productId.getOrElse(record.productId),
      quantity = quantity.orElse(record.quantity),
      costAmount = costAmount.orElse(record.costAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
