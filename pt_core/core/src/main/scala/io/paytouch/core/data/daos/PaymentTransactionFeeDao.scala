package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ PaymentTransactionFeeRecord, PaymentTransactionFeeUpdate }
import io.paytouch.core.data.tables.PaymentTransactionFeesTable

class PaymentTransactionFeeDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {
  type Record = PaymentTransactionFeeRecord
  type Update = PaymentTransactionFeeUpdate
  type Table = PaymentTransactionFeesTable

  val table = TableQuery[Table]

  def findByPaymentTransactionIds(paymentTransactionIds: Seq[UUID]): Future[Seq[PaymentTransactionFeeRecord]] =
    if (paymentTransactionIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByPaymentTransactionIds(paymentTransactionIds)
        .pipe(run)

  def queryFindByPaymentTransactionIds(paymentTransactionIds: Seq[UUID]) =
    table
      .filter(_.paymentTransactionId inSet paymentTransactionIds)
      .result
}
