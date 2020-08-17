package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ShiftStatus extends EnumEntrySnake

case object ShiftStatus extends Enum[ShiftStatus] {

  case object Published extends ShiftStatus
  case object Draft extends ShiftStatus

  val values = findValues
}
