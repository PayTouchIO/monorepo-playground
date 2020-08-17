package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ReturnOrderReason, UnitType }

final case class ReturnOrderProductRecord(
    id: UUID,
    merchantId: UUID,
    returnOrderId: UUID,
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    quantity: Option[BigDecimal],
    reason: ReturnOrderReason,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ReturnOrderProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    returnOrderId: Option[UUID],
    productId: Option[UUID],
    productName: Option[String],
    productUnit: Option[UnitType],
    quantity: Option[BigDecimal],
    reason: Option[ReturnOrderReason],
  ) extends SlickMerchantUpdate[ReturnOrderProductRecord] {

  def toRecord: ReturnOrderProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert ReturnOrderProductUpdate without a merchant id. [$this]")
    require(
      returnOrderId.isDefined,
      s"Impossible to convert ReturnOrderProductUpdate without a return order id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert ReturnOrderProductUpdate without a product id. [$this]")
    require(productName.isDefined, s"Impossible to convert ReturnOrderProductUpdate without a product name. [$this]")
    require(productUnit.isDefined, s"Impossible to convert ReturnOrderProductUpdate without a product unit. [$this]")
    require(reason.isDefined, s"Impossible to convert ReturnOrderProductUpdate without a reason. [$this]")
    ReturnOrderProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      returnOrderId = returnOrderId.get,
      productId = productId.get,
      productName = productName.get,
      productUnit = productUnit.get,
      quantity = quantity,
      reason = reason.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ReturnOrderProductRecord): ReturnOrderProductRecord =
    ReturnOrderProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      returnOrderId = returnOrderId.getOrElse(record.returnOrderId),
      productId = productId.getOrElse(record.productId),
      productName = productName.getOrElse(record.productName),
      productUnit = productUnit.getOrElse(record.productUnit),
      quantity = quantity.orElse(record.quantity),
      reason = reason.getOrElse(record.reason),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
