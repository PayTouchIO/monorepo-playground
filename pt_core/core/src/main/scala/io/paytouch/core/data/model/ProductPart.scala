package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ProductPartRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    partId: UUID,
    quantityNeeded: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord

case class ProductPartUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    partId: Option[UUID],
    quantityNeeded: Option[BigDecimal],
  ) extends SlickProductUpdate[ProductPartRecord] {

  def toRecord: ProductPartRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductPartUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert ProductPartUpdate without a product id. [$this]")
    require(partId.isDefined, s"Impossible to convert ProductPartUpdate without a part id. [$this]")
    require(
      quantityNeeded.isDefined,
      s"Impossible to convert ProductQuantityNeededUpdate without a quantity needed. [$this]",
    )
    ProductPartRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      partId = partId.get,
      quantityNeeded = quantityNeeded.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ProductPartRecord): ProductPartRecord =
    ProductPartRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      partId = partId.getOrElse(record.partId),
      quantityNeeded = quantityNeeded.getOrElse(record.quantityNeeded),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
