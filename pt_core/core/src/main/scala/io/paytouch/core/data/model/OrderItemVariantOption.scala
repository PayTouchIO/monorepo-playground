package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class OrderItemVariantOptionRecord(
    id: UUID,
    merchantId: UUID,
    orderItemId: UUID,
    variantOptionId: Option[UUID],
    optionName: String,
    optionTypeName: String,
    position: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOrderItemRelationRecord

case class OrderItemVariantOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderItemId: Option[UUID],
    variantOptionId: Option[UUID],
    optionName: Option[String],
    optionTypeName: Option[String],
    position: Option[Int],
  ) extends SlickMerchantUpdate[OrderItemVariantOptionRecord] {

  def toRecord: OrderItemVariantOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderItemVariantOptionUpdate without a merchant id. [$this]")
    require(
      orderItemId.isDefined,
      s"Impossible to convert OrderItemVariantOptionUpdate without a order item id. [$this]",
    )
    require(optionName.isDefined, s"Impossible to convert OrderItemVariantOptionUpdate without a option name. [$this]")
    require(
      optionTypeName.isDefined,
      s"Impossible to convert OrderItemVariantOptionUpdate without a option type name. [$this]",
    )
    require(position.isDefined, s"Impossible to convert OrderItemVariantOptionUpdate without a position. [$this]")
    OrderItemVariantOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderItemId = orderItemId.get,
      variantOptionId = variantOptionId,
      optionName = optionName.get,
      optionTypeName = optionTypeName.get,
      position = position.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderItemVariantOptionRecord): OrderItemVariantOptionRecord =
    OrderItemVariantOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      variantOptionId = variantOptionId.orElse(record.variantOptionId),
      optionName = optionName.getOrElse(record.optionName),
      optionTypeName = optionTypeName.getOrElse(record.optionTypeName),
      position = position.getOrElse(record.position),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
