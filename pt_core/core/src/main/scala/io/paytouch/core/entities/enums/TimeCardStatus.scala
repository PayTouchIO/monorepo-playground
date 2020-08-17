package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TimeCardStatus extends EnumEntrySnake

case object TimeCardStatus extends Enum[TimeCardStatus] {

  case object Open extends TimeCardStatus
  case object Closed extends TimeCardStatus

  val values = findValues
}
