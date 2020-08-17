package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.UnitType

final case class ReceivingOrderProductRecord(
    id: UUID,
    merchantId: UUID,
    receivingOrderId: UUID,
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    quantity: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ReceivingOrderProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    receivingOrderId: Option[UUID],
    productId: Option[UUID],
    productName: Option[String],
    productUnit: Option[UnitType],
    quantity: Option[BigDecimal],
    costAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[ReceivingOrderProductRecord] {

  def toRecord: ReceivingOrderProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert ReceivingOrderProductUpdate without a merchant id. [$this]")
    require(
      receivingOrderId.isDefined,
      s"Impossible to convert ReceivingOrderProductUpdate without a receiving order id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert ReceivingOrderProductUpdate without a product id. [$this]")
    require(productName.isDefined, s"Impossible to convert ReceivingOrderProductUpdate without a product name. [$this]")
    require(productUnit.isDefined, s"Impossible to convert ReceivingOrderProductUpdate without a product unit. [$this]")
    ReceivingOrderProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      receivingOrderId = receivingOrderId.get,
      productId = productId.get,
      productName = productName.get,
      productUnit = productUnit.get,
      quantity = quantity,
      costAmount = costAmount,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ReceivingOrderProductRecord): ReceivingOrderProductRecord =
    ReceivingOrderProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      receivingOrderId = receivingOrderId.getOrElse(record.receivingOrderId),
      productId = productId.getOrElse(record.productId),
      productName = productName.getOrElse(record.productName),
      productUnit = productUnit.getOrElse(record.productUnit),
      quantity = quantity.orElse(record.quantity),
      costAmount = costAmount.orElse(record.costAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
