package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.data.model.enums.DiscountType.Percentage
import io.paytouch.core.utils.EnumEntrySnake

sealed trait DiscountType extends EnumEntrySnake {
  val hasCurrency = this != Percentage
}

case object DiscountType extends Enum[DiscountType] {

  case object Percentage extends DiscountType
  case object FixedAmount extends DiscountType
  case object CustomPrice extends DiscountType

  val values = findValues
}
