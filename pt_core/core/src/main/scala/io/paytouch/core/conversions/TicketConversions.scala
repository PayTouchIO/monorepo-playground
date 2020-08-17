package io.paytouch.core.conversions

import java.util.UUID

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core._
import io.paytouch.core.data._
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.utils.UtcTime

trait TicketConversions extends ModelConversion[entities.TicketUpdate, model.TicketUpdate] with LazyLogging {
  def fromRecordsAndOptionsToEntities(
      records: Seq[model.TicketRecord],
      orderItemsPerTicket: Map[model.TicketRecord, Seq[entities.OrderItem]],
      bundleOrderItemsPerOrderId: Map[UUID, Seq[entities.OrderItem]],
      orderPerTicket: Option[Map[model.TicketRecord, entities.Order]],
      kitchens: Map[UUID, entities.Kitchen],
    ): Seq[entities.Ticket] =
    records.map { record =>
      val orderItems = orderItemsPerTicket.getOrElse(record, Seq.empty)
      val bundleOrderItems = bundleOrderItemsPerOrderId.getOrElse(record.orderId, Seq.empty)
      val order = orderPerTicket.flatMap(_.get(record))
      fromRecordAndOptionsToEntity(record, orderItems, bundleOrderItems, order, kitchens)
    }

  def fromRecordAndOptionsToEntity(
      record: model.TicketRecord,
      orderItems: Seq[entities.OrderItem],
      bundleOrderItems: Seq[entities.OrderItem],
      order: Option[entities.Order],
      kitchens: Map[UUID, entities.Kitchen],
    ): entities.Ticket =
    entities.Ticket(
      id = record.id,
      locationId = record.locationId,
      orderId = record.orderId,
      status = record.status,
      show = record.show,
      routeToKitchenId = record.routeToKitchenId,
      orderItems = orderItems,
      bundleOrderItems = bundleOrderItems,
      order = order,
      startedAt = record.startedAt,
      completedAt = record.completedAt,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      update: entities.TicketUpdate,
    )(implicit
      user: entities.UserContext,
    ): model.TicketUpdate =
    model.TicketUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      locationId = update.locationId,
      orderId = update.orderId,
      status = update.status,
      show = update.show,
      routeToKitchenId = update.routeToKitchenId,
      startedAt = None,
      completedAt = None,
    )

  def toTicketUpdateEntity(status: TicketStatus, show: Boolean): entities.TicketUpdate =
    entities.TicketUpdate(
      locationId = None,
      orderId = None,
      orderItemIds = None,
      status = status.some,
      show = show.some,
      routeToKitchenId = None,
    )
}
