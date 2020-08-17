package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType

final case class TransferOrderProductRecord(
    id: UUID,
    merchantId: UUID,
    transferOrderId: UUID,
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    quantity: Option[BigDecimal],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class TransferOrderProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    transferOrderId: Option[UUID],
    productId: Option[UUID],
    productName: Option[String],
    productUnit: Option[UnitType],
    quantity: Option[BigDecimal],
  ) extends SlickMerchantUpdate[TransferOrderProductRecord] {

  def toRecord: TransferOrderProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert TransferOrderProductUpdate without a merchant id. [$this]")
    require(
      transferOrderId.isDefined,
      s"Impossible to convert TransferOrderProductUpdate without a transfer order id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert TransferOrderProductUpdate without a product id. [$this]")
    require(productName.isDefined, s"Impossible to convert TransferOrderProductUpdate without a product name. [$this]")
    require(productUnit.isDefined, s"Impossible to convert TransferOrderProductUpdate without a product unit. [$this]")
    TransferOrderProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      transferOrderId = transferOrderId.get,
      productId = productId.get,
      productName = productName.get,
      productUnit = productUnit.get,
      quantity = quantity,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TransferOrderProductRecord): TransferOrderProductRecord =
    TransferOrderProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      transferOrderId = transferOrderId.getOrElse(record.transferOrderId),
      productId = productId.getOrElse(record.productId),
      productName = productName.getOrElse(record.productName),
      productUnit = productUnit.getOrElse(record.productUnit),
      quantity = quantity.orElse(record.quantity),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
