package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CommentType extends EnumEntrySnake

case object CommentType extends Enum[CommentType] {

  case object InventoryCount extends CommentType
  case object PurchaseOrder extends CommentType
  case object ReceivingOrder extends CommentType
  case object ReturnOrder extends CommentType

  val values = findValues
}
