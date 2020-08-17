package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities.Default

final case class CartItemVariantOptionRecord(
    id: UUID,
    storeId: UUID,
    cartItemId: UUID,
    variantOptionId: UUID,
    optionName: String,
    optionTypeName: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickStoreRecord

final case class CartItemVariantOptionUpdate(
    id: Default[UUID],
    storeId: Option[UUID],
    cartItemId: Option[UUID],
    variantOptionId: Option[UUID],
    optionName: Option[String],
    optionTypeName: Option[String],
  ) extends SlickUpdate[CartItemVariantOptionRecord] {

  def toRecord: CartItemVariantOptionRecord = {
    requires(
      "store id" -> storeId,
      "cart item id" -> cartItemId,
      "variant option id" -> variantOptionId,
      "option name" -> optionName,
      "option type name" -> optionTypeName,
    )
    CartItemVariantOptionRecord(
      id = id.getOrDefault,
      storeId = storeId.get,
      cartItemId = cartItemId.get,
      variantOptionId = variantOptionId.get,
      optionName = optionName.get,
      optionTypeName = optionTypeName.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CartItemVariantOptionRecord): CartItemVariantOptionRecord =
    CartItemVariantOptionRecord(
      id = id.getOrElse(record.id),
      storeId = storeId.getOrElse(record.storeId),
      cartItemId = cartItemId.getOrElse(record.cartItemId),
      variantOptionId = variantOptionId.getOrElse(record.variantOptionId),
      optionName = optionName.getOrElse(record.optionName),
      optionTypeName = optionTypeName.getOrElse(record.optionTypeName),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
