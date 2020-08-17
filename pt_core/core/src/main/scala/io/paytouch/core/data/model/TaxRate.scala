package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class TaxRateRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    value: BigDecimal,
    applyToPrice: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class TaxRateUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    name: Option[String],
    value: Option[BigDecimal],
    applyToPrice: Option[Boolean],
  ) extends SlickMerchantUpdate[TaxRateRecord] {

  def toRecord: TaxRateRecord = {
    require(merchantId.isDefined, s"Impossible to convert TaxRateUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert TaxRateUpdate without a name. [$this]")
    require(value.isDefined, s"Impossible to convert TaxRateUpdate without a value. [$this]")
    TaxRateRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      value = value.get,
      applyToPrice = applyToPrice.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TaxRateRecord): TaxRateRecord =
    TaxRateRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      value = value.getOrElse(record.value),
      applyToPrice = applyToPrice.getOrElse(record.applyToPrice),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
