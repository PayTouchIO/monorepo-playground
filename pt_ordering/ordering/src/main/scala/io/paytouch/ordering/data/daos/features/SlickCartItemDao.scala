package io.paytouch.ordering.data.daos.features

import java.util.UUID

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.tables.features.{ CartItemIdColumn, SlickTable }

import scala.concurrent.Future

trait SlickCartItemDao extends SlickDao {

  type Table <: SlickTable[Record] with CartItemIdColumn

  def findByCartItemIds(cartItemIds: Seq[UUID]): Future[Seq[Record]] = {
    val query = table.filter(_.cartItemId inSet cartItemIds)
    run(query.result)
  }

  def findByCartItemRelIds(cartItemRelIds: Seq[UUID]): Future[Seq[Record]] = {
    val query = table.filter(_.cartItemRelId inSet cartItemRelIds)
    run(query.result)
  }

  def queryBulkUpsertAndDeleteTheRestByCartItemId(updates: Seq[Update], cartItemId: UUID) =
    for {
      us <- queryBulkUpsert(updates)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.cartItemId === cartItemId)
    } yield records

}
