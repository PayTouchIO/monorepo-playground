package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickOrderItemDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderItemDiscountRecord, OrderItemDiscountUpdate }
import io.paytouch.core.data.tables.OrderItemDiscountsTable

import scala.concurrent.ExecutionContext

class OrderItemDiscountDao(implicit val ec: ExecutionContext, val db: Database) extends SlickOrderItemDao {

  type Record = OrderItemDiscountRecord
  type Update = OrderItemDiscountUpdate
  type Table = OrderItemDiscountsTable

  val table = TableQuery[Table]

}
