package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OrderTaxRateRecord(
    id: UUID,
    merchantId: UUID,
    orderId: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class OrderTaxRateUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderId: Option[UUID],
    taxRateId: Option[UUID],
    name: Option[String],
    value: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[OrderTaxRateRecord] {

  def toRecord: OrderTaxRateRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderTaxRateUpdate without a merchant id. [$this]")
    require(orderId.isDefined, s"Impossible to convert OrderTaxRateUpdate without a order id. [$this]")
    require(name.isDefined, s"Impossible to convert OrderTaxRateUpdate without a name. [$this]")
    require(value.isDefined, s"Impossible to convert OrderTaxRateUpdate without a value. [$this]")
    require(totalAmount.isDefined, s"Impossible to convert OrderTaxRateUpdate without a total amount. [$this]")
    OrderTaxRateRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderId = orderId.get,
      taxRateId = taxRateId,
      name = name.get,
      value = value.get,
      totalAmount = totalAmount.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderTaxRateRecord): OrderTaxRateRecord =
    OrderTaxRateRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderId = orderId.getOrElse(record.orderId),
      taxRateId = taxRateId.orElse(record.taxRateId),
      name = name.getOrElse(record.name),
      value = value.getOrElse(record.value),
      totalAmount = totalAmount.getOrElse(record.totalAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
