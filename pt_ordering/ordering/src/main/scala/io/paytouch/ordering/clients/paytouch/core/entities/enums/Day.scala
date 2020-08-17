package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait Day extends EnumEntrySnake

case object Day extends Enum[Day] {

  case object Sunday extends Day
  case object Monday extends Day
  case object Tuesday extends Day
  case object Wednesday extends Day
  case object Thursday extends Day
  case object Friday extends Day
  case object Saturday extends Day

  val values = findValues

}
