package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ModifierOptionRecord(
    id: UUID,
    merchantId: UUID,
    modifierSetId: UUID,
    name: String,
    priceAmount: BigDecimal,
    position: Int,
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ModifierOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    modifierSetId: Option[UUID],
    name: Option[String],
    priceAmount: Option[BigDecimal],
    position: Option[Int],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[ModifierOptionRecord] {

  def toRecord: ModifierOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert ModifierOptionUpdate without a merchant id. [$this]")
    require(modifierSetId.isDefined, s"Impossible to convert ModifierOptionUpdate without a modifier set id. [$this]")
    require(name.isDefined, s"Impossible to convert ModifierOptionUpdate without a name. [$this]")
    require(priceAmount.isDefined, s"Impossible to convert ModifierOptionUpdate without a price amount. [$this]")
    ModifierOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      modifierSetId = modifierSetId.get,
      name = name.get,
      priceAmount = priceAmount.get,
      position = position.getOrElse(0),
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ModifierOptionRecord): ModifierOptionRecord =
    ModifierOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      modifierSetId = modifierSetId.getOrElse(record.modifierSetId),
      name = name.getOrElse(record.name),
      priceAmount = priceAmount.getOrElse(record.priceAmount),
      position = position.getOrElse(record.position),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
