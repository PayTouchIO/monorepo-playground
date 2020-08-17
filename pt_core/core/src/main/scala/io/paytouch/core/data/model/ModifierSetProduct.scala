package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID
import io.paytouch.core.entities.{ ResettableInt }

final case class ModifierSetProductRecord(
    id: UUID,
    merchantId: UUID,
    modifierSetId: UUID,
    productId: UUID,
    position: Option[Int],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord

case class ModifierSetProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    modifierSetId: Option[UUID],
    productId: Option[UUID],
    position: ResettableInt,
  ) extends SlickProductUpdate[ModifierSetProductRecord] {

  def toRecord: ModifierSetProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert ModifierSetProductUpdate without a merchant id. [$this]")
    require(
      modifierSetId.isDefined,
      s"Impossible to convert ModifierSetProductUpdate without a modifier set id. [$this]",
    )
    require(productId.isDefined, s"Impossible to convert ModifierSetProductUpdate without a product id. [$this]")
    ModifierSetProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      modifierSetId = modifierSetId.get,
      productId = productId.get,
      position = position,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ModifierSetProductRecord): ModifierSetProductRecord =
    ModifierSetProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      modifierSetId = modifierSetId.getOrElse(record.modifierSetId),
      productId = productId.getOrElse(record.productId),
      position = position.getOrElse(record.position),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
