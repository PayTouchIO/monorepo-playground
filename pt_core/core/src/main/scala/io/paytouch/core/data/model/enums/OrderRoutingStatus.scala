package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait OrderRoutingStatus extends EnumEntrySnake

case object OrderRoutingStatus extends Enum[OrderRoutingStatus] {

  case object New extends OrderRoutingStatus
  case object Started extends OrderRoutingStatus
  case object Completed extends OrderRoutingStatus
  case object Canceled extends OrderRoutingStatus

  val values = findValues
}
