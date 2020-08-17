package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

final case class InventoryCountProductRecord(
    id: UUID,
    merchantId: UUID,
    inventoryCountId: UUID,
    productId: UUID,
    productName: String,
    expectedQuantity: Option[BigDecimal],
    countedQuantity: Option[BigDecimal],
    valueAmount: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    valueChangeAmount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class InventoryCountProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    inventoryCountId: Option[UUID],
    productId: Option[UUID],
    productName: Option[String],
    expectedQuantity: Option[BigDecimal],
    countedQuantity: Option[BigDecimal],
    valueAmount: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    valueChangeAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[InventoryCountProductRecord] {

  def toRecord: InventoryCountProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert InventoryCountProductUpdate without a merchant id. [$this]")
    require(
      inventoryCountId.isDefined,
      s"Impossible to convert InventoryCountProductUpdate without a inventory count id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert InventoryCountProductUpdate without a product id. [$this]")
    require(productName.isDefined, s"Impossible to convert InventoryCountProductUpdate without a product name. [$this]")
    InventoryCountProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      inventoryCountId = inventoryCountId.get,
      productId = productId.get,
      productName = productName.get,
      expectedQuantity = expectedQuantity.orElse(Some(0.0)),
      countedQuantity = countedQuantity.orElse(Some(0.0)),
      valueAmount = valueAmount,
      costAmount = costAmount,
      valueChangeAmount = valueChangeAmount,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: InventoryCountProductRecord): InventoryCountProductRecord =
    InventoryCountProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      inventoryCountId = inventoryCountId.getOrElse(record.inventoryCountId),
      productId = productId.getOrElse(record.productId),
      productName = productName.getOrElse(record.productName),
      expectedQuantity = expectedQuantity.orElse(record.expectedQuantity),
      countedQuantity = countedQuantity.orElse(record.countedQuantity),
      valueAmount = valueAmount.orElse(record.valueAmount),
      costAmount = costAmount.orElse(record.costAmount),
      valueChangeAmount = valueChangeAmount.orElse(record.valueChangeAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
