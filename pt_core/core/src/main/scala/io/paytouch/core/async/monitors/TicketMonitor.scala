package io.paytouch.core.async.monitors

import akka.actor.Actor

import cats.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.TicketService
import io.paytouch.core.utils._

final case class TicketChange(
    state: TicketService#State,
    update: TicketUpdate,
    resultType: ResultType,
    entity: Ticket,
    userContext: UserContext,
  )

class TicketMonitor(messageHandler: SQSMessageHandler) extends Actor {
  def receive: Receive = {
    case TicketChange(state, _, ResultType.Created, entity, user) =>
      implicit val u = user
      sendTicketCreated(entity)
      sendOrderUpdated(entity)
      sendOrderItemsUpdated(state, entity)

    case TicketChange(state, _, ResultType.Updated, entity, user) =>
      implicit val u = user
      sendTicketUpdated(entity)
      sendOrderUpdated(entity)
      sendOrderItemsUpdated(state, entity)
  }

  private def sendTicketCreated(entity: Ticket)(implicit user: UserContext): Unit =
    messageHandler.sendTicketCreatedMsg(entity)

  private def sendTicketUpdated(entity: Ticket)(implicit user: UserContext): Unit =
    messageHandler.sendTicketUpdatedMsg(entity)

  private def sendOrderUpdated(entity: Ticket)(implicit user: UserContext): Unit =
    entity.order.foreach(messageHandler.sendOrderUpdatedMsg)

  private def sendOrderItemsUpdated(oldOrderItems: Seq[OrderItem], entity: Ticket)(implicit user: UserContext): Unit =
    entity
      .orderItems
      .filterNot { current =>
        oldOrderItems.exists { old =>
          old.id === current.id && old.orderRoutingStatus == current.orderRoutingStatus
        }
      }
      .foreach { orderItem =>
        messageHandler.sendOrderItemUpdatedMsg(orderItem, entity.locationId)
      }
}
