package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._

import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed abstract class CartItemType extends EnumEntrySnake

case object CartItemType extends Enum[CartItemType] {
  case object GiftCard extends CartItemType
  case object Product extends CartItemType

  val values = findValues
}
