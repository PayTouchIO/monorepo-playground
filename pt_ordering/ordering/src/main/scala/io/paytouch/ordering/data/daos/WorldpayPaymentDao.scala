package io.paytouch.ordering.data.daos

import java.util.UUID

import io.paytouch.ordering.data.daos.features._
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ WorldpayPaymentRecord, WorldpayPaymentType, WorldpayPaymentUpdate }
import io.paytouch.ordering.data.tables.WorldpayPaymentsTable
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import io.paytouch.ordering.filters.NoFilters

import scala.concurrent.{ ExecutionContext, Future }

class WorldpayPaymentDao(implicit val ec: ExecutionContext, val db: Database) extends SlickUpsertDao {

  type Filters = NoFilters
  type Record = WorldpayPaymentRecord
  type Table = WorldpayPaymentsTable
  type Update = WorldpayPaymentUpdate
  type Upsertion = Update

  val table = TableQuery[Table]

  def findByTransactionSetupId(transactionSetupId: String): Future[Option[Record]] =
    first(queryByTransactionSetupId(transactionSetupId))

  def queryByTransactionSetupId(transactionSetupId: String) =
    table.filter(_.transactionSetupId === transactionSetupId)

  def findByCartId(cartId: UUID): Future[Option[Record]] =
    findById(cartId, WorldpayPaymentType.Cart)

  def findByPaymentIntentId(paymentIntentId: UUID): Future[Option[Record]] =
    findById(paymentIntentId, WorldpayPaymentType.PaymentIntent)

  private def first(query: Query[Table, Record, Seq]): Future[Option[Record]] =
    run(query.take(1).result.headOption)

  private def findById(objectId: UUID, objectType: WorldpayPaymentType) =
    first(
      table
        .filter(t =>
          all(
            Some(t.objectType === objectType),
            Some(t.objectId === objectId),
          ),
        ),
    )
}
