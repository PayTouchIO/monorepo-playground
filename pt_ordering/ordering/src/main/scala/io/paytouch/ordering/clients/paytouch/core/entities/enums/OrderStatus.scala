package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait OrderStatus extends EnumEntrySnake

case object OrderStatus extends Enum[OrderStatus] {

  case object Received extends OrderStatus
  case object InProgress extends OrderStatus
  case object InKitchen extends OrderStatus
  case object KitchenComplete extends OrderStatus
  case object InBar extends OrderStatus
  case object BarComplete extends OrderStatus
  case object Ready extends OrderStatus
  case object PickedUp extends OrderStatus
  case object EnRoute extends OrderStatus
  case object Delivered extends OrderStatus
  case object Completed extends OrderStatus
  case object Canceled extends OrderStatus

  val values = findValues
}
