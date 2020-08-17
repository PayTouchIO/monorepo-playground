package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickOrderItemRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderItemModifierOptionRecord, OrderItemModifierOptionUpdate }
import io.paytouch.core.data.tables.OrderItemModifierOptionsTable

import scala.concurrent.ExecutionContext

class OrderItemModifierOptionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickOrderItemRelDao {

  type Record = OrderItemModifierOptionRecord
  type Update = OrderItemModifierOptionUpdate
  type Table = OrderItemModifierOptionsTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.orderItemId.isDefined,
      "OrderItemModifierOptionDao - Impossible to find by order item id and modifier option id without a order item id",
    )
    queryFindByOrderItemIdAndModifierOptionId(upsertion.orderItemId.get, upsertion.modifierOptionId)
  }

  private def queryFindByOrderItemIdAndModifierOptionId(orderItemId: UUID, modifierOptionId: Option[UUID]) =
    table.filter(_.orderItemId === orderItemId).filter(_.modifierOptionId === modifierOptionId)
}
