package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.TicketStatus

final case class TicketRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    orderId: UUID,
    status: TicketStatus,
    show: Boolean,
    routeToKitchenId: UUID,
    startedAt: Option[ZonedDateTime],
    completedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {
  def hasStarted: Boolean =
    status.isNotNew

  def hasCompleted: Boolean =
    status.isCompleted

  def isNewOrInProgress: Boolean =
    status.isNewOrInProgress
}

case class TicketUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    orderId: Option[UUID],
    status: Option[TicketStatus],
    show: Option[Boolean],
    routeToKitchenId: Option[UUID],
    startedAt: Option[ZonedDateTime],
    completedAt: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[TicketRecord] {

  def toRecord: TicketRecord = {
    require(merchantId.isDefined, s"Impossible to convert TicketUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert TicketUpdate without a location id. [$this]")
    require(orderId.isDefined, s"Impossible to convert TicketUpdate without a order id. [$this]")
    require(status.isDefined, s"Impossible to convert TicketUpdate without a status. [$this]")
    require(routeToKitchenId.isDefined, s"Impossible to convert TicketUpdate without route to kitchen id. [$this]")

    TicketRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      orderId = orderId.get,
      status = status.get,
      show = show.getOrElse(true),
      routeToKitchenId = routeToKitchenId.get,
      startedAt = startedAt,
      completedAt = completedAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TicketRecord): TicketRecord =
    TicketRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      orderId = orderId.getOrElse(record.orderId),
      status = status.getOrElse(record.status),
      show = show.getOrElse(record.show),
      routeToKitchenId = routeToKitchenId.getOrElse(record.routeToKitchenId),
      startedAt = startedAt.orElse(record.startedAt),
      completedAt = completedAt.orElse(record.completedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
