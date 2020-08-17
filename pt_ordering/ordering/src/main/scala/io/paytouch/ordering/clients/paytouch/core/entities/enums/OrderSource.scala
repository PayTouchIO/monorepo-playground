package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait OrderSource extends EnumEntrySnake

case object OrderSource extends Enum[OrderSource] {

  case object Storefront extends OrderSource

  val values = findValues
}
