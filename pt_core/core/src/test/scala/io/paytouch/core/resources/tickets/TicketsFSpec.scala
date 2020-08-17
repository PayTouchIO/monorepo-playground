package io.paytouch.core.resources.tickets

import java.util.UUID

import io.paytouch.core.data.model.enums.OrderStatus
import io.paytouch.core.data.model.{ OrderItemRecord, OrderRecord, TicketRecord }
import io.paytouch.core.entities.{ Ticket, TicketCreation, TicketUpdate }
import io.paytouch.core.utils._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

abstract class TicketsFSpec extends FSpec {

  abstract class TicketResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    Factory.locationSettings(london).create
    lazy val kitchen = Factory.kitchen(london, name = Some("Kitchen")).create
    lazy val kitchenKdsDisabled = Factory.kitchen(london, name = Some("Kitchen"), kdsEnabled = Some(false)).create
    lazy val deletedKitchen =
      Factory.kitchen(london, name = Some("Deleted Kitchen"), deletedAt = Some(UtcTime.now)).create
    lazy val bar = Factory.kitchen(london, name = Some("Bar")).create

    val orderDao = daos.orderDao
    val ticketDao = daos.ticketDao
    val ticketOrderItemDao = daos.ticketOrderItemDao

    def assertResponseById(
        recordId: UUID,
        entity: Ticket,
        orderItems: Seq[OrderItemRecord],
        order: Option[OrderRecord],
      ) = {
      val record = ticketDao.findById(recordId).await.get
      assertResponse(entity, record, orderItems, order = order)
    }

    def assertResponse(
        entity: Ticket,
        record: TicketRecord,
        orderItems: Seq[OrderItemRecord],
        bundleOrderItems: Seq[OrderItemRecord] = Seq.empty,
        order: Option[OrderRecord] = None,
      ) = {
      entity.id ==== record.id
      entity.locationId ==== record.locationId
      entity.orderId ==== record.orderId
      entity.status ==== record.status
      entity.show ==== record.show
      entity.routeToKitchenId ==== record.routeToKitchenId
      entity.orderItems.map(_.id) should containTheSameElementsAs(orderItems.map(_.id))
      entity.bundleOrderItems.map(_.id) should containTheSameElementsAs(bundleOrderItems.map(_.id))
      entity.order.map(_.id) ==== order.map(_.id)
      entity.startedAt ==== record.startedAt
      entity.completedAt ==== record.completedAt
    }

    def assertCreation(recordId: UUID, creation: TicketCreation) = {
      assertUpdate(recordId, creation.asUpdate)

      afterAWhile {
        val orderRecord = orderDao.findById(creation.orderId).await.get
        orderRecord.status ==== Some(OrderStatus.InProgress)
      }
    }

    def assertUpdate(recordId: UUID, update: TicketUpdate) = {
      val record = ticketDao.findById(recordId).await.get
      val orderItemIds = ticketOrderItemDao.findByTicketId(record.id).await.map(_.orderItemId).distinct

      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.orderId.isDefined) update.orderId ==== Some(record.orderId)
      if (update.orderItemIds.isDefined) update.orderItemIds.get should containTheSameElementsAs(orderItemIds)
      if (update.status.isDefined) update.status ==== Some(record.status)
      if (update.show.isDefined) update.show ==== Some(record.show)
    }

  }
}
