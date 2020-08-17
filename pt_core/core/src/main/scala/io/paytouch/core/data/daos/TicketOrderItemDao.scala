package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ TicketOrderItemRecord, TicketOrderItemUpdate }
import io.paytouch.core.data.tables.TicketOrderItemsTable

import scala.concurrent._

class TicketOrderItemDao(implicit val ec: ExecutionContext, val db: Database) extends SlickRelDao {

  type Record = TicketOrderItemRecord
  type Update = TicketOrderItemUpdate
  type Table = TicketOrderItemsTable

  val table = TableQuery[Table]

  def queryByRelIds(update: Update) = {
    require(
      update.ticketId.isDefined,
      "TicketOrderItemDao - Impossible to find by ticket id and order item id without a ticket id",
    )
    require(
      update.orderItemId.isDefined,
      "TicketOrderItemDao - Impossible to find by ticket id and order item id without a order item id",
    )
    queryFindByTicketIdAndOrderItemId(update.ticketId.get, update.orderItemId.get)
  }

  def queryFindByTicketIdAndOrderItemId(ticketId: UUID, orderItemId: UUID) =
    table.filter(_.ticketId === ticketId).filter(_.orderItemId === orderItemId)

  def queryFindByTicketIds(ticketIds: Seq[UUID]) = table.filter(_.ticketId inSet ticketIds)

  def findByTicketId(ticketId: UUID): Future[Seq[Record]] = {
    val q = queryFindByTicketIds(Seq(ticketId))
    run(q.result)
  }

}
