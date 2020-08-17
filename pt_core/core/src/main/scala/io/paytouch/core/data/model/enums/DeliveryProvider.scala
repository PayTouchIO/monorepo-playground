package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.entities.enums.CustomerSource
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class DeliveryProvider extends EnumEntrySnake {
  final def toCustomerSourceType: CustomerSource =
    this match {
      case DeliveryProvider.UberEats  => CustomerSource.UberEats
      case DeliveryProvider.DoorDash  => CustomerSource.DoorDash
      case DeliveryProvider.Postmates => CustomerSource.Postmates
    }
}

case object DeliveryProvider extends Enum[DeliveryProvider] {
  case object UberEats extends DeliveryProvider
  case object DoorDash extends DeliveryProvider
  case object Postmates extends DeliveryProvider

  val values = findValues
}
