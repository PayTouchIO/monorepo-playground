package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReceivingOrderPaymentMethod extends EnumEntrySnake

case object ReceivingOrderPaymentMethod extends Enum[ReceivingOrderPaymentMethod] {

  case object Cash extends ReceivingOrderPaymentMethod
  case object Card extends ReceivingOrderPaymentMethod
  case object Check extends ReceivingOrderPaymentMethod

  val values = findValues
}
