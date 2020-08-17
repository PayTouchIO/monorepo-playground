package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.TicketOrderItemConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OrderItemRecord, TicketOrderItemUpdate, TicketRecord }
import io.paytouch.core.entities.{ OrderItem, TicketUpdate, UserContext }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.OrderItemValidator

import scala.concurrent._

class TicketOrderItemService(
    val orderItemService: OrderItemService,
    val ticketService: TicketService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TicketOrderItemConversions {

  val orderItemValidator = new OrderItemValidator

  def getOrderItemsPerTickets(
      tickets: Seq[TicketRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[TicketRecord, Seq[OrderItem]]] =
    orderItemService.findByTickets(tickets)

  def getBundleOrderItemsPerOrderId(
      tickets: Seq[TicketRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[OrderItem]]] =
    orderItemService.findBundleItemsByOrderId(tickets)

  def getTicketsPerOrderItem(orderItems: Seq[OrderItemRecord]): Future[Map[OrderItemRecord, Seq[TicketRecord]]] =
    ticketService.findByOrderItems(orderItems)

  def convertToTicketOrderItemUpdates(
      ticketId: UUID,
      update: TicketUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[TicketOrderItemUpdate]]]] =
    update.orderItemIds match {
      case Some(orderItemIds) =>
        orderItemValidator.accessByIds(orderItemIds).mapNested { _ =>
          val updates = toTicketOrderItemUpdates(ticketId, orderItemIds)
          Some(updates)
        }
      case None => Future.successful(Multiple.empty)
    }

}
