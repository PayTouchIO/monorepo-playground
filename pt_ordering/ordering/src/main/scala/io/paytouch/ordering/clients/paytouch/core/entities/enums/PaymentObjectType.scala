package io.paytouch.ordering.entities.enums

import enumeratum._

sealed trait PaymentObjectType extends EnumEntrySnake

case object PaymentObjectType extends Enum[PaymentObjectType] {

  case object Cart extends PaymentObjectType
  case object PaymentIntent extends PaymentObjectType

  val values = findValues
}
