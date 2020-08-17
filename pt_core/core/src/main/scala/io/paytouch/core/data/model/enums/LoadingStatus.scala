package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait LoadingStatus extends EnumEntrySnake

case object LoadingStatus extends Enum[LoadingStatus] {

  case object NotStarted extends LoadingStatus
  case object InProgress extends LoadingStatus
  case object Failed extends LoadingStatus
  case object Successful extends LoadingStatus

  val values = findValues
}
