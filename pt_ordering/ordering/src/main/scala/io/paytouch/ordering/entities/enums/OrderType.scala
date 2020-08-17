package io.paytouch.ordering.entities.enums

import enumeratum._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ OrderType => CoreOrderType }

sealed abstract class OrderType(val coreOrderType: CoreOrderType) extends EnumEntrySnake

case object OrderType extends Enum[OrderType] {

  case object TakeOut extends OrderType(CoreOrderType.TakeOut)
  case object Delivery extends OrderType(CoreOrderType.DeliveryRestaurant)

  val values = findValues
}
