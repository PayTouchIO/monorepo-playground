package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CashDrawerStatus extends EnumEntrySnake

case object CashDrawerStatus extends Enum[CashDrawerStatus] {

  case object Created extends CashDrawerStatus
  case object Started extends CashDrawerStatus
  case object Ended extends CashDrawerStatus

  val values = findValues
}
