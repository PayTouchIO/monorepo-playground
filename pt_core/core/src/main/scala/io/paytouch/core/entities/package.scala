package io.paytouch.core

import java.util.UUID

import scala.collection.immutable.Map
import io.paytouch.core.data.model.enums.{ KitchenType, OrderRoutingStatus }

package object entities {
  type BillingDetails = LegalDetails
  val BillingDetails = LegalDetails

  type OrderRoutingStatusesByType = Map[KitchenType, Option[OrderRoutingStatus]]

  object OrderRoutingStatusesByType {
    def apply(statuses: (KitchenType, Option[OrderRoutingStatus])*): OrderRoutingStatusesByType =
      Map(statuses: _*)
  }

  type OrderRoutingStatusesByKitchen = Map[UUID, Option[OrderRoutingStatus]]
}
