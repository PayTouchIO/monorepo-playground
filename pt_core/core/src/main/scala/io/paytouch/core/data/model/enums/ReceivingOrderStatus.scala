package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReceivingOrderStatus extends EnumEntrySnake

case object ReceivingOrderStatus extends Enum[ReceivingOrderStatus] {

  case object Receiving extends ReceivingOrderStatus
  case object Received extends ReceivingOrderStatus

  val values = findValues
}
