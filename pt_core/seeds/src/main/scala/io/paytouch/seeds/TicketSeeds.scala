package io.paytouch.seeds

import java.util.UUID

import scala.concurrent._

import org.scalacheck.Gen

import io.paytouch.core.data.model._
import io.paytouch.seeds.IdsProvider._

object TicketSeeds extends Seeds {
  lazy val ticketDao = daos.ticketDao

  def load(orders: Seq[OrderRecord])(implicit user: UserRecord): Future[Seq[TicketRecord]] = {
    val ticketIds = ticketIdsPerEmail(user.email)

    val ordersWithLocations = orders.filter(_.locationId.isDefined)

    val tickets = ticketIds.map { ticketId =>
      val order = ordersWithLocations.random

      val status = genTicketStatus.instance

      val startedAt = genZonedDateTime.instance
      val completedAt = {
        val mins = Gen.chooseNum(5, 60).sample.get
        startedAt.plusMinutes(mins)
      }

      TicketUpdate(
        id = Some(ticketId),
        merchantId = Some(user.merchantId),
        locationId = order.locationId,
        orderId = Some(order.id),
        status = Some(status),
        show = genOptBoolean.instance,
        routeToKitchenId = Some(UUID.randomUUID), // FIXME after kitchen seeds have been created
        startedAt = if (status.isNewOrInProgress) Some(startedAt) else None,
        completedAt = if (status.isCompleted) Some(completedAt) else None,
      )
    }

    ticketDao.bulkUpsert(tickets).extractRecords
  }
}
