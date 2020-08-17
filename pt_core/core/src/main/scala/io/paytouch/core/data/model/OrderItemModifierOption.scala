package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.ModifierSetType

final case class OrderItemModifierOptionRecord(
    id: UUID,
    merchantId: UUID,
    orderItemId: UUID,
    modifierOptionId: Option[UUID],
    name: String,
    modifierSetName: Option[String],
    `type`: ModifierSetType,
    priceAmount: BigDecimal,
    quantity: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOrderItemRelationRecord

case class OrderItemModifierOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    orderItemId: Option[UUID],
    modifierOptionId: Option[UUID],
    name: Option[String],
    modifierSetName: Option[String],
    `type`: Option[ModifierSetType],
    priceAmount: Option[BigDecimal],
    quantity: Option[BigDecimal],
  ) extends SlickMerchantUpdate[OrderItemModifierOptionRecord] {

  def toRecord: OrderItemModifierOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert OrderItemModifierOptionUpdate without a merchant id. [$this]")
    require(
      orderItemId.isDefined,
      s"Impossible to convert OrderItemModifierOptionUpdate without a order item id. [$this]",
    )
    require(name.isDefined, s"Impossible to convert OrderItemModifierOptionUpdate without a name. [$this]")
    require(`type`.isDefined, s"Impossible to convert OrderItemModifierOptionUpdate without a type. [$this]")
    require(
      priceAmount.isDefined,
      s"Impossible to convert OrderItemModifierOptionUpdate without a price amount. [$this]",
    )
    require(quantity.isDefined, s"Impossible to convert OrderItemModifierOptionUpdate without a quantity. [$this]")
    OrderItemModifierOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      orderItemId = orderItemId.get,
      modifierOptionId = modifierOptionId,
      name = name.get,
      modifierSetName = modifierSetName,
      `type` = `type`.get,
      priceAmount = priceAmount.get,
      quantity = quantity.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: OrderItemModifierOptionRecord): OrderItemModifierOptionRecord =
    OrderItemModifierOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      modifierOptionId = modifierOptionId.orElse(record.modifierOptionId),
      name = name.getOrElse(record.name),
      modifierSetName = modifierSetName.orElse(record.modifierSetName),
      `type` = `type`.getOrElse(record.`type`),
      priceAmount = priceAmount.getOrElse(record.priceAmount),
      quantity = quantity.getOrElse(record.quantity),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
