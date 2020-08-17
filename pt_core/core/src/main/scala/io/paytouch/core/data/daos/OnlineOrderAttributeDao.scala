package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.AcceptanceStatus
import io.paytouch.core.data.tables.OnlineOrderAttributesTable
import io.paytouch.core.utils.UtcTime

class OnlineOrderAttributeDao(orderDao: => OrderDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao {
  type Record = OnlineOrderAttributeRecord
  type Update = OnlineOrderAttributeUpdate
  type Table = OnlineOrderAttributesTable

  val table = TableQuery[Table]

  def queryFilterByAcceptanceStatus(acceptanceStatus: AcceptanceStatus) =
    table.filter(_.acceptanceStatus === acceptanceStatus)

  def findByOrderId(orderId: UUID): Future[Option[Record]] =
    queryFindByOrderId(orderId)
      .result
      .pipe(run)
      .map(_.headOption)

  private def queryFindByOrderId(orderId: UUID) =
    table
      .filter(
        _.id in orderDao
          .queryOpenByIds(Seq(orderId))
          .map(_.onlineOrderAttributeId),
      )
}
