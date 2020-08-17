package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ImportType extends EnumEntrySnake

case object ImportType extends Enum[ImportType] {

  case object Product extends ImportType

  val values = findValues
}
