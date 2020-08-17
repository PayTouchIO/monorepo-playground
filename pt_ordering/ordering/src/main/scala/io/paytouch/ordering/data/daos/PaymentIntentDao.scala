package io.paytouch.ordering.data.daos

import java.util.UUID

import io.paytouch.ordering.data.daos.features._
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ PaymentIntentRecord, PaymentIntentUpdate }
import io.paytouch.ordering.data.tables.PaymentIntentsTable
import io.paytouch.ordering.filters.NoFilters

import scala.concurrent.{ ExecutionContext, Future }

class PaymentIntentDao(implicit val ec: ExecutionContext, val db: Database) extends SlickUpsertDao {

  type Filters = NoFilters
  type Record = PaymentIntentRecord
  type Table = PaymentIntentsTable
  type Update = PaymentIntentUpdate
  type Upsertion = Update

  val table = TableQuery[Table]
}
