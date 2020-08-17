package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ PaymentTransactionRecord, PaymentTransactionUpdate }
import io.paytouch.core.data.tables.PaymentTransactionsTable

class PaymentTransactionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {
  type Record = PaymentTransactionRecord
  type Update = PaymentTransactionUpdate
  type Table = PaymentTransactionsTable

  val table = TableQuery[Table]

  def findByOrderIds(orderIds: Seq[UUID]): Future[Seq[PaymentTransactionRecord]] =
    if (orderIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByOrderIds(orderIds)
        .pipe(run)

  def queryFindByOrderIds(orderIds: Seq[UUID]) =
    table.filter(_.orderId inSet orderIds).result

  def querySearchByCardDigits(q: String) =
    table
      .filter(t => querySearchByLast4Digits(t, q) || querySearchByMaskPan(t, q))

  private def querySearchByLast4Digits(t: Table, q: String) =
    t.paymentDetailsJson.+>>("last4Digits") like s"%${q}"

  private def querySearchByMaskPan(t: Table, q: String) =
    t.paymentDetailsJson.+>>("maskPan") like s"%${q}"
}
