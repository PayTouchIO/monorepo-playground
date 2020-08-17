package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait FulfillmentStatus extends EnumEntrySnake

case object FulfillmentStatus extends Enum[FulfillmentStatus] {

  case object Unfulfilled extends FulfillmentStatus
  case object Fulfilled extends FulfillmentStatus
  case object PartiallyFulfilled extends FulfillmentStatus
  case object Canceled extends FulfillmentStatus

  val values = findValues
}
