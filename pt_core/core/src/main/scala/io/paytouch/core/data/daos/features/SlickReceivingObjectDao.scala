package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.ReceivingObjectStatusColumn
import io.paytouch.core.data.model.SlickReceivingObjectRecord
import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.data.tables.SlickMerchantTable
import io.paytouch.core.entities.enums.ReceivingOrderView
import io.paytouch.core.utils.UtcTime

import scala.concurrent._

trait SlickReceivingObjectDao extends SlickMerchantDao {
  type Table <: SlickMerchantTable[Record] with ReceivingObjectStatusColumn
  type Record <: SlickReceivingObjectRecord

  protected def queryFilterByOptionalView(view: Option[ReceivingOrderView]): Query[Table, Record, Seq] =
    view match {
      case Some(ReceivingOrderView.Complete)   => baseQuery.filter(hasStatusCompleted)
      case Some(ReceivingOrderView.Incomplete) => baseQuery.filterNot(hasStatusCompleted)

      // N.b. this is ignored for transfer orders. filterAvailableForReturn is
      // overriden in purchaseOrderDao.
      case Some(ReceivingOrderView.AvailableForReturn) => filterAvailableForReturn

      case _ => baseQuery
    }

  protected def filterAvailableForReturn: Query[Table, Record, Seq] =
    baseQuery

  private def hasStatusCompleted(t: Table): Rep[Boolean] =
    t.status === (ReceivingObjectStatus.Completed: ReceivingObjectStatus)

  def missingQuantitiesPerProductId(id: UUID): Future[Map[UUID, BigDecimal]]

  def setStatus(id: UUID, status: ReceivingObjectStatus): Future[Int] =
    runWithTransaction {
      table
        .filter(_.id === id)
        .map(o => o.status -> o.updatedAt)
        .update(status, UtcTime.now)
    }
}
