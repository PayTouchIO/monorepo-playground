package io.paytouch.ordering.clients.worldpay.entities

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait ResponseCode extends EnumEntrySnake

case object ResponseCode extends Enum[ResponseCode] {

  case object Ok extends ResponseCode
  case object NoRecord extends ResponseCode
  case object InvalidRequest extends ResponseCode

  case object UnknownError extends ResponseCode
  case object ResponseParseError extends ResponseCode

  val values = findValues
}
