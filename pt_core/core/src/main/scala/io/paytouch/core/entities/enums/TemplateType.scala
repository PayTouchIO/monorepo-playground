package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait TemplateType extends EnumEntrySnake

case object TemplateType extends Enum[TemplateType] {

  case object Loyalty extends TemplateType
  case object GiftCard extends TemplateType

  val values = findValues
}
