package io.paytouch.ordering.clients.google.entities

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait GStatus extends EnumEntrySnake

case object GStatus extends Enum[GStatus] {

  case object Ok extends GStatus
  case object NotFound extends GStatus
  case object ZeroResults extends GStatus
  case object MaxWaypointsExceeded extends GStatus
  case object MaxRouteLengthExceeded extends GStatus
  case object InvalidRequest extends GStatus
  case object OverQueryLimit extends GStatus
  case object RequestDenied extends GStatus
  case object UnknownError extends GStatus

  val values = findValues
}
