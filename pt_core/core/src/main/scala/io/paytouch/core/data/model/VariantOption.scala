package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class VariantOptionRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    variantOptionTypeId: UUID,
    name: String,
    position: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickProductRecord

case class VariantOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    variantOptionTypeId: Option[UUID],
    name: Option[String],
    position: Option[Int],
  ) extends SlickProductUpdate[VariantOptionRecord] {

  def toRecord: VariantOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert VariantOptionUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert VariantOptionUpdate without a product id. [$this]")
    require(
      variantOptionTypeId.isDefined,
      s"Impossible to convert VariantOptionUpdate without a variant option type id. [$this]",
    )
    require(name.isDefined, s"Impossible to convert VariantOptionUpdate without a name. [$this]")
    require(position.isDefined, s"Impossible to convert VariantOptionUpdate without a position. [$this]")
    VariantOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      variantOptionTypeId = variantOptionTypeId.get,
      name = name.get,
      position = position.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: VariantOptionRecord): VariantOptionRecord =
    VariantOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      variantOptionTypeId = variantOptionTypeId.getOrElse(record.variantOptionTypeId),
      name = name.getOrElse(record.name),
      position = position.getOrElse(record.position),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
