package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderUserRecord, OrderUserUpdate }
import io.paytouch.core.data.tables.OrderUsersTable

import scala.concurrent.ExecutionContext

class OrderUserDao(val userDao: UserDao)(implicit val ec: ExecutionContext, val db: Database) extends SlickRelDao {

  type Record = OrderUserRecord
  type Update = OrderUserUpdate
  type Table = OrderUsersTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: Update) = {
    require(upsertion.orderId.isDefined, "OrderUserDao - Impossible to find by order id and user id without a order id")
    require(upsertion.userId.isDefined, "OrderUserDao - Impossible to find by order id and user id without a user id")
    queryFindByOrderIdAndUserId(upsertion.orderId.get, upsertion.userId.get)
  }

  private def queryFindByOrderIdAndUserId(orderId: UUID, userId: UUID) =
    table.filter(_.orderId === orderId).filter(_.userId === userId)

  def findByOrderIds(orderIds: Seq[UUID]) =
    run(queryFindByOrderIds(orderIds).result)

  def queryFindByOrderIds(orderIds: Seq[UUID]) =
    table.filter(_.orderId inSet orderIds)
}
