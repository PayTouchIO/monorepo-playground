package io.paytouch.core.filters

import java.time.{ LocalDateTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.View
import io.paytouch.core.entities.UserContext

final case class OrderFilters(
    locationIds: Seq[UUID] = Seq.empty,
    customerId: Option[UUID] = None,
    tableId: Option[UUID] = None,
    paymentType: Option[OrderPaymentType] = None,
    view: Option[View] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    query: Option[String] = None,
    isInvoice: Option[Boolean] = None,
    orderStatus: Option[Seq[OrderStatus]] = None,
    acceptanceStatus: Option[AcceptanceStatus] = None,
    paymentStatus: Option[PaymentStatus] = None,
    sourcesOrDeliveryProviders: Option[Either[Seq[Source], Seq[DeliveryProvider]]] = None,
    updatedSince: Option[ZonedDateTime] = None,
  ) extends BaseFilters

object OrderFilters {
  def withAccessibleLocations(
      locationId: Option[UUID],
      customerId: Option[UUID],
      tableId: Option[UUID],
      paymentType: Option[OrderPaymentType],
      view: Option[View],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      isInvoice: Boolean,
      orderStatus: Option[Seq[OrderStatus]],
      acceptanceStatus: Option[AcceptanceStatus],
      paymentStatus: Option[PaymentStatus],
      sourcesOrDeliveryProviders: Option[Either[Seq[Source], Seq[DeliveryProvider]]],
      updatedSince: Option[ZonedDateTime],
    )(implicit
      user: UserContext,
    ): OrderFilters = {
    val locationIds = user.accessibleLocations(locationId)
    OrderFilters(
      locationIds,
      customerId,
      tableId,
      paymentType,
      view,
      from,
      to,
      query,
      Some(isInvoice),
      orderStatus,
      acceptanceStatus,
      paymentStatus,
      sourcesOrDeliveryProviders,
      updatedSince,
    )
  }
}
