package io.paytouch.ordering.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.ordering.data.daos.features._
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ MerchantRecord, MerchantUpdate, PaymentProcessorConfig }
import io.paytouch.ordering.data.tables.MerchantsTable
import io.paytouch.ordering.filters.NoFilters

class MerchantDao(implicit val ec: ExecutionContext, val db: Database) extends SlickUpsertDao {
  type Filters = NoFilters
  type Record = MerchantRecord
  type Table = MerchantsTable
  type Update = MerchantUpdate
  type Upsertion = Update

  val table = TableQuery[Table]

  def existsUrlSlug(idToExclude: UUID, urlSlug: String): Future[Boolean] =
    queryByNotId(idToExclude)
      .filter(_.urlSlug === urlSlug)
      .exists
      .result
      .pipe(run)

  def existsUrlSlug(urlSlug: String): Future[Boolean] =
    table
      .filter(_.urlSlug === urlSlug)
      .exists
      .result
      .pipe(run)

  def queryFindBySlug(slug: String) =
    table.filter(_.urlSlug === slug)

  def findBySlug(slug: String): Future[Option[Record]] =
    queryFindBySlug(slug)
      .result
      .headOption
      .pipe(run)

  def findPaymentProcessorConfig(merchantId: UUID): Future[Option[PaymentProcessorConfig]] =
    queryById(merchantId)
      .map(_.paymentProcessorConfig)
      .result
      .headOption
      .pipe(run)
}
