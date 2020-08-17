package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities.Default
import io.paytouch.ordering.entities.enums.ModifierSetType

final case class CartItemModifierOptionRecord(
    id: UUID,
    storeId: UUID,
    cartItemId: UUID,
    modifierOptionId: UUID,
    name: String,
    `type`: ModifierSetType,
    priceAmount: BigDecimal,
    quantity: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickStoreRecord

final case class CartItemModifierOptionUpdate(
    id: Default[UUID],
    storeId: Option[UUID],
    cartItemId: Option[UUID],
    modifierOptionId: Option[UUID],
    name: Option[String],
    `type`: Option[ModifierSetType],
    priceAmount: Option[BigDecimal],
    quantity: Option[BigDecimal],
  ) extends SlickUpdate[CartItemModifierOptionRecord] {

  def toRecord: CartItemModifierOptionRecord = {
    requires(
      "store id" -> storeId,
      "cart item id" -> cartItemId,
      "modifier option id" -> modifierOptionId,
      "name" -> name,
      "type" -> `type`,
      "price amount" -> priceAmount,
      "quantity" -> quantity,
    )
    CartItemModifierOptionRecord(
      id = id.getOrDefault,
      storeId = storeId.get,
      cartItemId = cartItemId.get,
      modifierOptionId = modifierOptionId.get,
      name = name.get,
      `type` = `type`.get,
      priceAmount = priceAmount.get,
      quantity = quantity.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CartItemModifierOptionRecord): CartItemModifierOptionRecord =
    CartItemModifierOptionRecord(
      id = id.getOrElse(record.id),
      storeId = storeId.getOrElse(record.storeId),
      cartItemId = cartItemId.getOrElse(record.cartItemId),
      modifierOptionId = modifierOptionId.getOrElse(record.modifierOptionId),
      name = name.getOrElse(record.name),
      `type` = `type`.getOrElse(record.`type`),
      priceAmount = priceAmount.getOrElse(record.priceAmount),
      quantity = quantity.getOrElse(record.quantity),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
