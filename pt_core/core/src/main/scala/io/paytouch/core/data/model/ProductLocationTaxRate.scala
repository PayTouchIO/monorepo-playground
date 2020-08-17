package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ProductLocationTaxRateRecord(
    id: UUID,
    merchantId: UUID,
    productLocationId: UUID,
    taxRateId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ProductLocationTaxRateUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productLocationId: Option[UUID],
    taxRateId: Option[UUID],
  ) extends SlickMerchantUpdate[ProductLocationTaxRateRecord] {

  def toRecord: ProductLocationTaxRateRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductLocationTaxRateUpdate without a merchant id. [$this]")
    require(
      productLocationId.isDefined,
      s"Impossible to convert ProductLocationTaxRateUpdate without a product location id. [$this]",
    )
    require(taxRateId.isDefined, s"Impossible to convert ProductLocationTaxRateUpdate without a tax rate id. [$this]")
    ProductLocationTaxRateRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productLocationId = productLocationId.get,
      taxRateId = taxRateId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ProductLocationTaxRateRecord): ProductLocationTaxRateRecord =
    ProductLocationTaxRateRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productLocationId = productLocationId.getOrElse(record.productLocationId),
      taxRateId = taxRateId.getOrElse(record.taxRateId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
