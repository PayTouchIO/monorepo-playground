package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderDiscountRecord, OrderDiscountUpdate }
import io.paytouch.core.data.tables.OrderDiscountsTable

class OrderDiscountDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {
  type Record = OrderDiscountRecord
  type Update = OrderDiscountUpdate
  type Table = OrderDiscountsTable

  val table = TableQuery[Table]

  def queryBulkUpsertAndDeleteTheRestByOrderId(upsertions: Seq[Update], orderId: UUID) =
    for {
      us <- queryBulkUpsert(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.orderId === orderId)
    } yield records

  def findByOrderIds(orderIds: Seq[UUID]): Future[Seq[Record]] =
    if (orderIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByOrderIds(orderIds)
        .result
        .pipe(run)

  private def queryFindByOrderIds(orderIds: Seq[UUID]) =
    table.filter(_.orderId inSet orderIds)
}
