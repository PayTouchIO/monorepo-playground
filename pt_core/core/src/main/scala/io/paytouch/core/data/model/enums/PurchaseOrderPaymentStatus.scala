package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait PurchaseOrderPaymentStatus extends EnumEntrySnake

case object PurchaseOrderPaymentStatus extends Enum[PurchaseOrderPaymentStatus] {

  case object Unpaid extends PurchaseOrderPaymentStatus
  case object Paid extends PurchaseOrderPaymentStatus
  case object Partial extends PurchaseOrderPaymentStatus

  val values = findValues
}
