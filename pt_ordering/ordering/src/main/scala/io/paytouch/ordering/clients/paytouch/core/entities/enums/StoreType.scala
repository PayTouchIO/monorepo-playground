package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait StoreType extends EnumEntrySnake

case object StoreType extends Enum[StoreType] {

  case object Storefront extends StoreType

  val values = findValues
}
