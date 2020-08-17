package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.upsertions.TicketUpsertion
import io.paytouch.core.data.model.{ TicketRecord, TicketUpdate }
import io.paytouch.core.data.tables.TicketsTable
import io.paytouch.core.entities.enums.TicketStatus
import io.paytouch.core.filters.TicketFilters
import io.paytouch.core.utils.ResultType

import scala.concurrent._

class TicketDao(
    val locationDao: LocationDao,
    val orderDao: OrderDao,
    val ticketOrderItemDao: TicketOrderItemDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao {

  type Record = TicketRecord
  type Update = TicketUpdate
  type Upsertion = TicketUpsertion
  type Filters = TicketFilters
  type Table = TicketsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      routeToKitchenIds = f.routeToKitchenIds,
      orderNumber = f.orderNumber,
      show = f.show,
      status = f.status,
    )(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      routeToKitchenIds: Option[Seq[UUID]],
      orderNumber: Option[String],
      show: Option[Boolean],
      status: Option[TicketStatus],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    run(
      queryFindAllByMerchantId(merchantId, locationIds, routeToKitchenIds, orderNumber, show, status)
        .drop(offset)
        .take(limit)
        .result,
    )

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      routeToKitchenIds = f.routeToKitchenIds,
      orderNumber = f.orderNumber,
      show = f.show,
      status = f.status,
    )

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      routeToKitchenIds: Option[Seq[UUID]],
      orderNumber: Option[String],
      show: Option[Boolean],
      status: Option[TicketStatus],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationIds, routeToKitchenIds, orderNumber, show, status).length.result)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      routeToKitchenIds: Option[Seq[UUID]],
      orderNumber: Option[String],
      show: Option[Boolean],
      status: Option[TicketStatus],
    ) =
    table
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          Some(t.locationId in locationDao.queryFindByIds(locationIds).map(_.id)),
          orderNumber.map(ordNum =>
            t.orderId in orderDao.queryFindOrderByMerchantIdAndNumber(merchantId, ordNum).map(_.id),
          ),
          show.map(sw => t.show === sw),
          status.map(st => t.status === st),
          routeToKitchenIds.map(kitchenIds => t.routeToKitchenId.inSet(kitchenIds)),
        ),
      )

  def findPerOrderIds(orderIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] = {
    val q = table.filter(_.orderId inSet orderIds)
    run(q.result).map(_.groupBy(_.orderId))
  }

  def findPerOrderItemIds(orderItemIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] = {
    val q = table.join(ticketOrderItemDao.table.filter(_.orderItemId inSet orderItemIds)).on(_.id === _.ticketId).map {
      case (ticketsT, ticketOrderItemsT) => ticketOrderItemsT.orderItemId -> ticketsT
    }

    run(q.result).map { result =>
      result.groupBy { case (orderItemId, _) => orderItemId }.transform { (_, v) =>
        v.map { case (_, ticket) => ticket }
      }
    }
  }

  def findByOrderId(orderId: UUID): Future[Seq[Record]] = {
    val q = table.filter(_.orderId === orderId)
    run(q.result)
  }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val q = for {
      (resultType, ticket) <- queryUpsert(upsertion.ticket)
      _ <- asOption(
        upsertion
          .ticketOrderItems
          .map(ticketOrderItemDao.queryBulkUpsertAndDeleteTheRestByRelIds(_, t => t.ticketId === ticket.id)),
      )
    } yield (resultType, ticket)
    runWithTransaction(q)
  }

  def findNonCompletedByOrderId(orderId: UUID): Future[Seq[Record]] = {
    val completed: Rep[TicketStatus] = TicketStatus.Completed
    val q = table.filter(_.orderId === orderId).filterNot(_.status === completed)
    run(q.result)
  }
}
