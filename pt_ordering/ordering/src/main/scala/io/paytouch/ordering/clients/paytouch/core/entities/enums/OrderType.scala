package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait OrderType extends EnumEntrySnake

case object OrderType extends Enum[OrderType] {

  case object TakeOut extends OrderType
  case object DeliveryRestaurant extends OrderType

  val values = findValues
}
