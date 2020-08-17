package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait InventoryCountStatus extends EnumEntrySnake

case object InventoryCountStatus extends Enum[InventoryCountStatus] {

  case object Matched extends InventoryCountStatus
  case object Unmatched extends InventoryCountStatus
  case object InProgress extends InventoryCountStatus

  val values = findValues
}
