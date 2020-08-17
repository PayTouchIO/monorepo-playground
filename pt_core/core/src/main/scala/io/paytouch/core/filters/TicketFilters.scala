package io.paytouch.core.filters

import java.util.UUID

import io.paytouch.core.entities.UserContext
import io.paytouch.core.entities.enums.TicketStatus

final case class TicketFilters(
    routeToKitchenIds: Option[Seq[UUID]] = None,
    locationIds: Seq[UUID] = Seq.empty,
    orderNumber: Option[String] = None,
    show: Option[Boolean] = None,
    status: Option[TicketStatus] = None,
  ) extends BaseFilters

object TicketFilters {
  def withAccessibleLocations(
      routeToKitchenIds: Seq[UUID],
      locationId: Option[UUID],
      orderNumber: Option[String],
      show: Option[Boolean],
      status: Option[TicketStatus],
    )(implicit
      user: UserContext,
    ): TicketFilters = {
    val locationIds = user.accessibleLocations(locationId)
    TicketFilters(Some(routeToKitchenIds), locationIds, orderNumber, show, status)
  }
}
