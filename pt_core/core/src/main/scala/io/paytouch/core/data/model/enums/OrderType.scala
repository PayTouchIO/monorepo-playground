package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait OrderType extends EnumEntrySnake

case object OrderType extends Enum[OrderType] {

  case object DineIn extends OrderType
  case object TakeOut extends OrderType
  case object DeliveryRestaurant extends OrderType
  case object InStore extends OrderType
  case object InStorePickUp extends OrderType
  case object DeliveryRetail extends OrderType

  val values = findValues

  def byBusinessType(businessType: BusinessType) =
    businessType match {
      case BusinessType.QSR        => Seq(OrderType.DineIn, OrderType.TakeOut, OrderType.DeliveryRestaurant)
      case BusinessType.Retail     => Seq(OrderType.InStore, OrderType.InStorePickUp, OrderType.DeliveryRetail)
      case BusinessType.Restaurant => Seq(OrderType.DineIn, OrderType.TakeOut, OrderType.DeliveryRestaurant)
    }
}
