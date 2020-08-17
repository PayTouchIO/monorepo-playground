package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ProductVariantOptionRecord(
    id: UUID,
    merchantId: UUID,
    variantOptionId: UUID,
    productId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord

case class ProductVariantOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    variantOptionId: Option[UUID],
    productId: Option[UUID],
  ) extends SlickProductUpdate[ProductVariantOptionRecord] {

  def toRecord: ProductVariantOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductVariantOptionUpdate without a merchant id. [$this]")
    require(
      variantOptionId.isDefined,
      s"Impossible to convert ProductVariantOptionUpdate without a variant option id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert ProductVariantOptionUpdate without a product id. [$this]")
    ProductVariantOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      variantOptionId = variantOptionId.get,
      productId = productId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ProductVariantOptionRecord): ProductVariantOptionRecord =
    ProductVariantOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      variantOptionId = variantOptionId.getOrElse(record.variantOptionId),
      productId = productId.getOrElse(record.productId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
