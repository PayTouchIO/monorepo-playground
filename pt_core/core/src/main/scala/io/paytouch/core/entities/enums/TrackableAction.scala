package io.paytouch.core.entities.enums

import enumeratum.Enum
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TrackableAction extends EnumEntrySnake

case object TrackableAction extends Enum[TrackableAction] {

  case object Created extends TrackableAction
  case object Updated extends TrackableAction
  case object Deleted extends TrackableAction

  val values = findValues
}
