package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OrderTaxRateRecord, OrderTaxRateUpdate }
import io.paytouch.core.data.tables.OrderTaxRatesTable

import scala.concurrent._

class OrderTaxRateDao(implicit val ec: ExecutionContext, val db: Database) extends SlickRelDao {

  type Record = OrderTaxRateRecord
  type Update = OrderTaxRateUpdate
  type Table = OrderTaxRatesTable

  val table = TableQuery[Table]

  def queryFindAllByOrderIds(ids: Seq[UUID]) =
    table.filter(_.orderId inSet ids)

  def findAllByOrderIds(ids: Seq[UUID]): Future[Seq[Record]] =
    run(queryFindAllByOrderIds(ids).result)

  def findOrderTaxRatesPerOrderIds(orderIds: Seq[UUID]): Future[Map[UUID, Seq[OrderTaxRateRecord]]] = {
    val q = queryFindAllByOrderIds(orderIds).map(t => t.orderId -> t)
    run(q.result).map(_.groupBy { case (orderId, _) => orderId }.transform { (_, v) =>
      v.map {
        case (_, orderTaxRateRecord) => orderTaxRateRecord
      }
    })
  }

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.orderId.isDefined,
      s"OrderTaxRateDao - Impossible to find by order id and tax rate id without a order id $upsertion",
    )
    queryFindByOrderIdAndTaxRateId(upsertion.orderId.get, upsertion.taxRateId)
  }

  private def queryFindByOrderIdAndTaxRateId(orderId: UUID, taxRateId: Option[UUID]) =
    table
      .filter(t =>
        all(
          Some(t.orderId === orderId),
          Some((t.taxRateId === taxRateId).getOrElse(false)),
          Some(t.taxRateId.isDefined),
        ),
      )
}
