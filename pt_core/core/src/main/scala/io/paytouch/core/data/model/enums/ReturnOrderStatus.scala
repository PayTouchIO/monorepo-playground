package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReturnOrderStatus extends EnumEntrySnake

case object ReturnOrderStatus extends Enum[ReturnOrderStatus] {

  case object Created extends ReturnOrderStatus
  case object Sent extends ReturnOrderStatus
  case object Canceled extends ReturnOrderStatus
  case object Rejected extends ReturnOrderStatus
  case object Accepted extends ReturnOrderStatus

  val values = findValues
}
