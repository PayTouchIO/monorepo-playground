package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickOrderItemRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderItemTaxRateRecord, OrderItemTaxRateUpdate }
import io.paytouch.core.data.tables.OrderItemTaxRatesTable

import scala.concurrent._

class OrderItemTaxRateDao(implicit val ec: ExecutionContext, val db: Database) extends SlickOrderItemRelDao {

  type Record = OrderItemTaxRateRecord
  type Update = OrderItemTaxRateUpdate
  type Table = OrderItemTaxRatesTable

  val table = TableQuery[Table]

  def queryFindAllByOrderItemIds(ids: Seq[UUID]) =
    table.filter(_.orderItemId inSet ids)

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.orderItemId.isDefined,
      "OrderItemTaxRateDao - Impossible to find by order item id and modifier option id without a order item id",
    )
    queryFindByOrderItemIdAndTaxRateId(upsertion.orderItemId.get, upsertion.taxRateId)
  }

  private def queryFindByOrderItemIdAndTaxRateId(orderItemId: UUID, taxRateId: Option[UUID]) =
    table.filter(_.orderItemId === orderItemId).filter(_.taxRateId === taxRateId)

  def findOrderItemTaxRatesPerOrderItemIds(orderItemIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] = {
    val q = queryFindAllByOrderItemIds(orderItemIds).map(t => t.orderItemId -> t)
    run(q.result).map(_.groupBy { case (orderItemId, _) => orderItemId }
      .transform { (_, v) =>
        v.map {
          case (_, orderItemTaxRateRecord) => orderItemTaxRateRecord
        }
      })
  }
}
