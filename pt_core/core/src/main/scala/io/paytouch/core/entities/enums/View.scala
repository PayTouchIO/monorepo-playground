package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait View extends EnumEntrySnake

case object View extends Enum[View] {

  case object Active extends View
  case object Completed extends View
  case object Canceled extends View
  case object Pending extends View

  val values = findValues
}
