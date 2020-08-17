package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickMerchantDao, SlickUaPassUrls }
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.GiftCardPassesTable
import io.paytouch.core.filters.GiftCardPassSalesSummaryFilters
import io.paytouch.core.services.GiftCardPassService
import io.paytouch.core.utils.UtcTime

class GiftCardPassDao(
    orderItemDao: => OrderItemDao,
    val giftCardPassTransactionDao: GiftCardPassTransactionDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickUaPassUrls {
  type Record = GiftCardPassRecord
  type Update = GiftCardPassUpdate
  type Table = GiftCardPassesTable

  type SalesReport = (Int, Int, BigDecimal)

  val table = TableQuery[Table]

  def findOneByMerchantIdAndLookupId(merchantId: UUID, lookupId: String): Future[Option[Record]] =
    table
      .filter(_.merchantId === merchantId)
      .filter(_.lookupId.toLowerCase === lookupId.toLowerCase)
      .result
      .headOption
      .pipe(run)

  def findOneByMerchantIdAndOnlineCode(
      merchantId: UUID,
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    ): Future[Option[Record]] =
    table
      .filter(_.merchantId === merchantId)
      .filter(_.onlineCode === onlineCode.value.hyphenless.toUpperCase)
      .result
      .headOption
      .pipe(run)

  def decreaseBalance(id: UUID, charge: BigDecimal): Future[Option[(Record, GiftCardPassTransactionRecord)]] =
    (for {
      record <- table.filter(_.id === id).forUpdate.result.headOption
      _ <- asOption(record.map(r => queryUpdateBalance(id, r.balanceAmount - charge)))
      updatedRecord <- queryFindById(id)
      transactionsWithResultType <- asOption(record.map(r => queryCreateTransaction(r, -charge)))
      transaction = transactionsWithResultType.headOption.map { case (_, r) => r }
    } yield updatedRecord -> transaction).pipe(runWithTransaction).map(_.tupled)

  def decreaseBalance(
      bulkCharge: Map[io.paytouch.GiftCardPass.Id, BigDecimal],
    ): Future[Seq[(Record, GiftCardPassTransactionRecord)]] =
    (for {
      records <- queryFindByIds(bulkCharge.keys.map(_.cast.get.value).toSeq).forUpdate.result
      _ <- asSeq(
        records.map { r =>
          queryUpdateBalance(
            r.id,
            r.balanceAmount - bulkCharge(io.paytouch.GiftCardPass.IdPostgres(r.id).cast),
          )
        },
      )
      updatedRecords <- queryFindByIds(bulkCharge.keys.map(_.cast.get.value).toSeq).forUpdate.result
      transactionsWithResultType <- asSeq(records.map { r =>
        queryCreateTransaction(r, -bulkCharge(io.paytouch.GiftCardPass.IdPostgres(r.id).cast))
      })
      transactions = transactionsWithResultType.map { case (_, r) => r }
    } yield updatedRecords.toList -> transactions.toList).pipe(runWithTransaction).map {
      case (records, transactions) =>
        records.zip(transactions)
    }

  private def queryCreateTransaction(record: Record, amount: BigDecimal) =
    GiftCardPassTransactionUpdate(
      id = None,
      merchantId = record.merchantId.some,
      giftCardPassId = record.id.some,
      totalAmount = amount.some,
    ).pipe(giftCardPassTransactionDao.queryUpsert)

  private def queryUpdateBalance(id: UUID, balance: BigDecimal) =
    table
      .filter(_.id === id)
      .map(o => o.balanceAmount -> o.updatedAt)
      .update(balance, UtcTime.now)

  def findByOrderItemId(orderItemId: UUID): Future[Option[Record]] =
    table
      .filter(_.orderItemId === orderItemId)
      .result
      .headOption
      .pipe(run)

  def findByOrderItemIds(orderItemIds: Seq[UUID]): Future[Seq[Record]] =
    table
      .filter(_.orderItemId inSet orderItemIds)
      .result
      .pipe(run)

  def doesOnlineCodeExist(onlineCode: GiftCardPass.OnlineCode): Future[Boolean] =
    table
      .filter(_.onlineCode === onlineCode.value)
      .exists
      .result
      .pipe(run)

  def updateRecipientEmail(id: UUID, recipientEmail: String): Future[Option[Record]] =
    table
      .filter(_.id === id)
      .map(o => o.recipientEmail -> o.updatedAt)
      .update(recipientEmail.some, UtcTime.now)
      .map(_ > 0)
      .flatMap(_ => queryFindById(id))
      .pipe(runWithTransaction)

  private val emptySalesReport: SalesReport = (0, 0, 0)

  def computeGiftCardPassSalesSummary(
      f: GiftCardPassSalesSummaryFilters,
    ): Future[(SalesReport, SalesReport, SalesReport)] =
    if (f.locationIds.isEmpty)
      Future.successful(emptySalesReport, emptySalesReport, emptySalesReport)
    else
      (for {
        purchased <- queryComputePurchasedSalesSummary(f.locationIds, f.from, f.to)
        used <- queryComputeUsedSalesSummary(f.locationIds, f.from, f.to)
      } yield (purchased, used)).pipe(run).map {
        case (purchased @ (pCount, pCustomers, pValue), used @ (uCount, uCustomers, uValue)) =>
          val unusedCount = Math.max(pCount - uCount, 0)
          val unusedCustomers = Math.max(pCustomers - uCustomers, 0)
          val unusedValue = BigDecimal(0).max(pValue - uValue)
          val unused = (unusedCount, unusedCustomers, unusedValue)

          (purchased, used, unused)
      }

  private def queryComputeUsedSalesSummary(
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) = {
    val tableAlias = "rep"

    val whereClauses = Seq(
      Some(s"$tableAlias.original_location_id IN (${locationIds.asInParametersList})"),
      Some(s"$tableAlias.gift_card_pass_transaction_id = ''"),
      from.map(start => s"$tableAlias.received_at_tz >= ${localDateTimeAsString(start)}"),
      to.map(end => s"$tableAlias.received_at_tz < ${localDateTimeAsString(end)}"),
    ).flatten

    sql"""
         SELECT COUNT(DISTINCT #$tableAlias.gift_card_pass_id),
                COUNT(DISTINCT transactions.customer_id),
                COALESCE(SUM(- transactions.charged_amount), 0)
         FROM reports_gift_card_pass_transactions AS #$tableAlias
         JOIN LATERAL (
            SELECT rep2.customer_id,
                   rep2.charged_amount
            FROM reports_gift_card_pass_transactions AS rep2
            WHERE #$tableAlias.gift_card_pass_id = rep2.gift_card_pass_id
            AND rep2.gift_card_pass_transaction_id != ''
            #${to.map(end => s"AND rep2.received_at_tz < ${localDateTimeAsString(end)}").getOrElse("")}
         ) transactions
         ON TRUE
         WHERE #${whereClauses.mkString(" AND ")}
       """.as[(Int, Int, BigDecimal)].head
  }

  private def queryComputePurchasedSalesSummary(
      locationIds: Seq[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) = {
    val tableAlias = "rep"

    val whereClauses = Seq(
      Some(s"$tableAlias.original_location_id IN (${locationIds.asInParametersList})"),
      Some(s"$tableAlias.gift_card_pass_transaction_id = ''"),
      from.map(start => s"$tableAlias.received_at_tz >= ${localDateTimeAsString(start)}"),
      to.map(end => s"$tableAlias.received_at_tz < ${localDateTimeAsString(end)}"),
    ).flatten

    sql"""
     SELECT COUNT(DISTINCT rep.gift_card_pass_id),
            COUNT(DISTINCT rep.customer_id),
            COALESCE(SUM(rep.remaining_amount), 0)
     FROM reports_gift_card_pass_transactions AS #$tableAlias
     WHERE #${whereClauses.mkString(" AND ")}
   """.as[(Int, Int, BigDecimal)].head
  }
}
