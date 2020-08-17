package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReceivingOrderView extends EnumEntrySnake

case object ReceivingOrderView extends Enum[ReceivingOrderView] {

  case object Incomplete extends ReceivingOrderView
  case object Complete extends ReceivingOrderView
  case object AvailableForReturn extends ReceivingOrderView // Only for purchaseOrders

  val values = findValues
}
