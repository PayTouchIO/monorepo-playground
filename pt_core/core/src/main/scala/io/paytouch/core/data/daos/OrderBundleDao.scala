package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderBundleRecord, OrderBundleUpdate }
import io.paytouch.core.data.tables.OrderBundlesTable

class OrderBundleDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {
  type Record = OrderBundleRecord
  type Update = OrderBundleUpdate
  type Table = OrderBundlesTable

  val table = TableQuery[Table]

  def queryBulkUpsertAndDeleteTheRestByOrderId(upsertions: Seq[Update], orderId: UUID) =
    for {
      us <- queryBulkUpsert(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.orderId === orderId)
    } yield records

  def findByOrderId(orderId: UUID): Future[Seq[Record]] =
    findByOrderIds(Seq(orderId))

  def findByOrderIds(orderIds: Seq[UUID]): Future[Seq[Record]] =
    if (orderIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByOrderIds(orderIds)
        .result
        .pipe(run)

  def queryFindByOrderIds(orderIds: Seq[UUID]) =
    table.filter(_.orderId inSet orderIds)
}
