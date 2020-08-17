package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait HandledVia extends EnumEntrySnake

case object HandledVia extends Enum[HandledVia] {

  case object Unhandled extends HandledVia
  case object CashDrawerActivity extends HandledVia
  case object TipsDistributed extends HandledVia
  case object MarkAsPaid extends HandledVia

  val values = findValues
}
