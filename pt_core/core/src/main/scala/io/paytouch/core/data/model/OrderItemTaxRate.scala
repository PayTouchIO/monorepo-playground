package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OrderItemTaxRateRecord(
    id: UUID,
    merchantId: UUID,
    orderItemId: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: Option[BigDecimal],
    applyToPrice: Boolean,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderItemTaxRateUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderItemId: Option[UUID],
    taxRateId: Option[UUID],
    name: Option[String],
    value: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
    applyToPrice: Option[Boolean],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[OrderItemTaxRateRecord] {

  def toRecord: OrderItemTaxRateRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderItemTaxRateUpdate without a merchant id. [$this]")
    require(orderItemId.isDefined, s"Impossible to convert OrderItemTaxRateUpdate without a order item id. [$this]")
    require(name.isDefined, s"Impossible to convert OrderItemTaxRateUpdate without a name. [$this]")
    require(value.isDefined, s"Impossible to convert OrderItemTaxRateUpdate without a value. [$this]")
    OrderItemTaxRateRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderItemId = orderItemId.get,
      taxRateId = taxRateId,
      name = name.get,
      value = value.get,
      totalAmount = totalAmount,
      applyToPrice = applyToPrice.getOrElse(false),
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderItemTaxRateRecord): OrderItemTaxRateRecord =
    OrderItemTaxRateRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      taxRateId = taxRateId.orElse(record.taxRateId),
      name = name.getOrElse(record.name),
      value = value.getOrElse(record.value),
      totalAmount = totalAmount.orElse(record.totalAmount),
      applyToPrice = applyToPrice.getOrElse(record.applyToPrice),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
