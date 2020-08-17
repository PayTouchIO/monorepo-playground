package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class OrderStatus extends EnumEntrySnake

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

  lazy val positiveValues = values filterNot Set(Canceled)
  lazy val notOpen: Set[OrderStatus] = Set(Received, Canceled, Completed)
  lazy val open: Set[OrderStatus] = values.filterNot(notOpen).toSet
}
