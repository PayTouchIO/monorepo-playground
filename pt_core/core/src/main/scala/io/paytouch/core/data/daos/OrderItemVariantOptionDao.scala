package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickOrderItemRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderItemVariantOptionRecord, OrderItemVariantOptionUpdate }
import io.paytouch.core.data.tables.OrderItemVariantOptionsTable

import scala.concurrent.ExecutionContext

class OrderItemVariantOptionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickOrderItemRelDao {

  type Record = OrderItemVariantOptionRecord
  type Update = OrderItemVariantOptionUpdate
  type Table = OrderItemVariantOptionsTable

  val table = TableQuery[Table]

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.orderItemId.isDefined,
      "OrderItemVariantOptionDao - Impossible to find by order item id and variant option id without a order item id",
    )
    queryFindByOrderItemIdAndUserId(upsertion.orderItemId.get, upsertion.variantOptionId)
  }

  private def queryFindByOrderItemIdAndUserId(orderItemId: UUID, variantOptionId: Option[UUID]) =
    table.filter(_.orderItemId === orderItemId).filter(_.variantOptionId === variantOptionId)
}
