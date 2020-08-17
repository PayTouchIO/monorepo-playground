package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CancellationStatus extends EnumEntrySnake

case object CancellationStatus extends Enum[CancellationStatus] {
  case object Requested extends CancellationStatus
  case object Acknowledged extends CancellationStatus

  val values = findValues
}
