package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OrderItemIdColumn
import io.paytouch.core.data.tables.SlickMerchantTable

import scala.concurrent._

trait SlickOrderItemRelDao extends SlickOrderItemDao with SlickRelDao

trait SlickOrderItemDao extends SlickMerchantDao {
  type Table <: SlickMerchantTable[Record] with OrderItemIdColumn

  def findAllByOrderItemIds(ids: Seq[UUID]): Future[Seq[Record]] =
    run(baseQuery.filter(_.orderItemId inSet ids).result)
}
