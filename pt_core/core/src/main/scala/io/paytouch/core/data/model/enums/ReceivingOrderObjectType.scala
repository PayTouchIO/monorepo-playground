package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ReceivingOrderObjectType extends EnumEntrySnake

case object ReceivingOrderObjectType extends Enum[ReceivingOrderObjectType] {

  case object PurchaseOrder extends ReceivingOrderObjectType
  case object Transfer extends ReceivingOrderObjectType

  val values = findValues
}
