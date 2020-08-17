package io.paytouch.core.entities

import io.paytouch.core.OrderWorkflow
import io.paytouch.core.data.model.enums.OrderStatus._
import io.paytouch.core.data.model.enums.{ KitchenType, OrderType }

object OrderWorkflow {
  def getByOrderType(orderType: OrderType, kitchens: Seq[Kitchen]): OrderWorkflow = {
    val hasBar = kitchens.exists(_.`type` == KitchenType.Bar)
    val hasKitchen = kitchens.exists(_.`type` == KitchenType.Kitchen)
    orderType match {
      case OrderType.DineIn if hasBar && hasKitchen => Seq(InKitchen, KitchenComplete, InBar, BarComplete)
      case OrderType.DineIn if hasKitchen           => Seq(InKitchen, KitchenComplete)
      case OrderType.DineIn if hasBar               => Seq(InBar, BarComplete)
      case OrderType.DineIn                         => Seq(InProgress, Completed)

      case OrderType.TakeOut if hasKitchen => Seq(Received, InKitchen, KitchenComplete, PickedUp, Completed)
      case OrderType.TakeOut               => Seq(Received, InProgress, Ready, PickedUp, Completed)

      case OrderType.DeliveryRestaurant if hasKitchen =>
        Seq(Received, InKitchen, KitchenComplete, EnRoute, Delivered, Completed)
      case OrderType.DeliveryRestaurant =>
        Seq(Received, InProgress, EnRoute, Delivered, Completed)

      case OrderType.InStore        => Seq(Completed)
      case OrderType.InStorePickUp  => Seq(Ready, Completed)
      case OrderType.DeliveryRetail => Seq(Ready, EnRoute, Delivered, Completed)
    }
  }
}
