package io.paytouch.ordering.data.daos.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.tables.features._

trait SlickCartDao extends SlickDao {
  type Table <: SlickTable[Record] with CartIdColumn

  def findByCartIds(cartIds: Seq[UUID]): Future[Seq[Record]] =
    table
      .filter(_.cartId inSet cartIds)
      .sortBy(_.createdAt)
      .result
      .pipe(run)
}
