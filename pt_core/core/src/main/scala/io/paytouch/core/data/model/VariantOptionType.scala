package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class VariantOptionTypeRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    name: String,
    position: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord

case class VariantOptionTypeUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    name: Option[String],
    position: Option[Int],
  ) extends SlickProductUpdate[VariantOptionTypeRecord] {

  def toRecord: VariantOptionTypeRecord = {
    require(merchantId.isDefined, s"Impossible to convert VariantOptionTypeUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert VariantOptionTypeUpdate without a product id. [$this]")
    require(name.isDefined, s"Impossible to convert VariantOptionTypeUpdate without a name. [$this]")
    require(position.isDefined, s"Impossible to convert VariantOptionTypeUpdate without a position. [$this]")
    VariantOptionTypeRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      name = name.get,
      position = position.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: VariantOptionTypeRecord): VariantOptionTypeRecord =
    VariantOptionTypeRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      name = name.getOrElse(record.name),
      position = position.getOrElse(record.position),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
