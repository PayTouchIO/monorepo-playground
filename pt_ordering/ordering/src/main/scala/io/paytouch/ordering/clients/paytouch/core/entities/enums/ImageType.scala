package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed abstract class ImageType extends EnumEntrySnake

case object ImageType extends Enum[ImageType] {

  case object StoreHero extends ImageType
  case object StoreLogo extends ImageType

  val values = findValues
}
