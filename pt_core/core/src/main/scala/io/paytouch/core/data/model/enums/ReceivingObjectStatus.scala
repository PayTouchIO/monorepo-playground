package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReceivingObjectStatus extends EnumEntrySnake

case object ReceivingObjectStatus extends Enum[ReceivingObjectStatus] {

  case object Created extends ReceivingObjectStatus
  case object Receiving extends ReceivingObjectStatus
  case object Partial extends ReceivingObjectStatus
  case object Completed extends ReceivingObjectStatus

  val values = findValues
}
