package io.paytouch.ordering.data.daos

import scala.concurrent._

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.data.daos.features._
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ PaymentProcessorCallbackRecord, PaymentProcessorCallbackUpdate }
import io.paytouch.ordering.data.tables.PaymentProcessorCallbacksTable
import io.paytouch.ordering.entities.enums.{ PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.filters.NoFilters

class PaymentProcessorCallbackDao(implicit val ec: ExecutionContext, val db: Database) extends SlickUpsertDao {
  type Filters = NoFilters
  type Record = PaymentProcessorCallbackRecord
  type Table = PaymentProcessorCallbacksTable
  type Update = PaymentProcessorCallbackUpdate
  type Upsertion = Update

  val table = TableQuery[Table]

  def findByPaymentProcessorAndReferenceAndStatus(
      paymentProcessor: PaymentProcessor,
      status: PaymentProcessorCallbackStatus,
      reference: Option[String],
    ): Future[Seq[Record]] =
    table
      .filter(_.paymentProcessor === paymentProcessor)
      .filter(_.status === status)
      .filter(t => reference.fold[Rep[Option[Boolean]]](t.reference.isEmpty.?)(t.reference === _))
      .result
      .pipe(run)

  def findByStripeWebhookId(webhookId: String): Future[Seq[Record]] =
    table
      .filter(_.paymentProcessor === (PaymentProcessor.Stripe: PaymentProcessor))
      // The following should actually be: `.filter(_.payload.#>>(List("body", "id")) === webhookId)`
      // but slick disagrees: https://github.com/tminglei/slick-pg/blob/master/addons/json4s/src/test/scala/com/github/tminglei/slickpg/PgJson4sSupportSuite.scala#L96
      .filter(_.payload +>> "body" like s"%$webhookId%")
      .result
      .pipe(run)
}
