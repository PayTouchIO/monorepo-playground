package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ GiftCardPassAggregate, ReportFields }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object GiftCardPassView extends GiftCardPassView

trait GiftCardPassView extends ReportAggrView {

  type GroupBy = GiftCardPassGroupBy
  type Field = GiftCardPassFields
  type AggrResult = GiftCardPassAggregate

  protected val aggrCsvConverter = giftCardPassAggregateConverter

  val fieldsEnum = GiftCardPassFields

  val endpoint = "gift_card_passes"

  override def tableName(filters: ReportFilters) = tableNameAlias
  override lazy val tableNameAlias = "reports_gift_card_pass_transactions"

  override lazy val idColumn = s"$tableNameAlias.merchant_id"
  override val countSelectors = Seq(s"$idColumn AS id", s"COUNT(DISTINCT $tableNameAlias.gift_card_pass_id) AS cnt")

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        data <- Option(GiftCardPassAggregate.getResultOrZero(count, r))
        key <- Option(r.nextStringOption())
      } yield ReportFields(values = data, key = key),
    )

  def expandView(filters: ReportFilters): Option[String] = None

  def locationClauses(locationIds: Seq[UUID]) =
    Seq(s"$tableNameAlias.original_location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.received_at_tz BETWEEN $from AND $to")

  val defaultClauses: Seq[String] = Seq(s"$tableNameAlias.gift_card_pass_transaction_id = ''")

  val transactionsJoinLateral =
    s"""
         JOIN LATERAL (
            SELECT COALESCE(SUM(rep2.charged_amount),0) AS charges
            FROM reports_gift_card_pass_transactions AS rep2
            WHERE rep2.gift_card_pass_transaction_id != ''
            AND rep2.received_at_tz < $intervalEndTimeSelector
            AND rep2.gift_card_pass_id = $tableNameAlias.gift_card_pass_id
         ) transactions ON TRUE
       """.stripMargin

  def defaultJoins(filters: ReportFilters) = {
    val clauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      locationClauses(filters.locationIds) ++ defaultClauses
    val joinLateral =
      s"""
         JOIN LATERAL (
            SELECT COALESCE(SUM(rep2.charged_amount),0) AS charges
            FROM reports_gift_card_pass_transactions AS rep2
            WHERE rep2.gift_card_pass_transaction_id != ''
            AND rep2.received_at_tz < $intervalEndTimeSelector
            AND rep2.gift_card_pass_id = $tableNameAlias.gift_card_pass_id
         ) transactions ON TRUE
       """.stripMargin

    Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${clauses.mkString(" AND ")}", joinLateral)
  }
}
