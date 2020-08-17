package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait NextNumberType extends EnumEntrySnake

case object NextNumberType extends Enum[NextNumberType] {

  case object InventoryCount extends NextNumberType
  case object Order extends NextNumberType
  case object PurchaseOrder extends NextNumberType
  case object ReceivingOrder extends NextNumberType
  case object ReturnOrder extends NextNumberType
  case object TransferOrder extends NextNumberType

  val values = findValues
}
