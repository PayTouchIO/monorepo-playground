package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ImportStatus extends EnumEntrySnake

case object ImportStatus extends Enum[ImportStatus] {

  case object NotStarted extends ImportStatus
  case object InProgress extends ImportStatus
  case object Failed extends ImportStatus
  case object Successful extends ImportStatus

  val values = findValues
}
