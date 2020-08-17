package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait ImageSize extends EnumEntrySnake

case object ImageSize extends Enum[ImageSize] {

  case object Original extends ImageSize
  case object Thumbnail extends ImageSize
  case object Small extends ImageSize
  case object Medium extends ImageSize
  case object Large extends ImageSize

  val values = findValues

}
