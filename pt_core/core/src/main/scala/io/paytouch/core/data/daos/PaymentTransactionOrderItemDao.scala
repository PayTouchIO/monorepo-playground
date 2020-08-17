package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickOrderItemRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ PaymentTransactionOrderItemRecord, PaymentTransactionOrderItemUpdate }
import io.paytouch.core.data.tables.PaymentTransactionOrderItemsTable

import scala.concurrent.ExecutionContext

class PaymentTransactionOrderItemDao(implicit val ec: ExecutionContext, val db: Database) extends SlickOrderItemRelDao {

  type Record = PaymentTransactionOrderItemRecord
  type Update = PaymentTransactionOrderItemUpdate
  type Table = PaymentTransactionOrderItemsTable

  val table = TableQuery[Table]

  def findByPaymentTransactionId(paymentTransactionId: UUID) = findByPaymentTransactionIds(Seq(paymentTransactionId))

  def findByPaymentTransactionIds(ids: Seq[UUID]) =
    run(table.filter(_.paymentTransactionId inSet ids).result)

  def queryByRelIds(upsertion: Update) = {
    require(
      upsertion.paymentTransactionId.isDefined,
      s"PaymentTransactionOrderItemDao - Impossible to find by payment transaction id and order item id without a payment transaction id $upsertion",
    )
    require(
      upsertion.orderItemId.isDefined,
      s"PaymentTransactionOrderItemDao - Impossible to find by payment transaction id and order item id without a order item id $upsertion",
    )
    queryFindByPaymentTransactionIdAndOrderItemId(upsertion.paymentTransactionId, upsertion.orderItemId)
  }

  private def queryFindByPaymentTransactionIdAndOrderItemId(
      paymentTransactionId: Option[UUID],
      orderItemId: Option[UUID],
    ) =
    table
      .filter(t =>
        all(
          Some((t.paymentTransactionId === paymentTransactionId).getOrElse(false)),
          Some((t.orderItemId === orderItemId).getOrElse(false)),
        ),
      )
}
