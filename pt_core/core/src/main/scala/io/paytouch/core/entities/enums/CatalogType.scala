package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake
import io.paytouch.core.utils.RichString._

sealed trait CatalogType extends EnumEntrySnake

case object CatalogType extends Enum[CatalogType] {

  case object Menu extends CatalogType
  case object DefaultMenu extends CatalogType

  val values = findValues
}
