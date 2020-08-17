package io.paytouch.core.entities.enums

import enumeratum._

import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class StoreType extends EnumEntrySnake

case object StoreType extends Enum[StoreType] {
  case object DeliveryProvider extends StoreType
  case object Storefront extends StoreType

  val values = findValues
}
