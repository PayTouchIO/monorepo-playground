package io.paytouch.ordering.entities.enums

import enumeratum._

sealed abstract class CartStatus extends EnumEntrySnake

case object CartStatus extends Enum[CartStatus] {
  case object New extends CartStatus
  case object Paid extends CartStatus

  val values = findValues
}
