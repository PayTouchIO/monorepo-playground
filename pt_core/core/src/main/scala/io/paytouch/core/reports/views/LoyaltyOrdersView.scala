package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ LoyaltyOrdersAggregate, ReportFields }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object LoyaltyOrdersView extends LoyaltyOrdersView

trait LoyaltyOrdersView extends ReportAggrView {

  type GroupBy = LoyaltyOrdersGroupBy
  type Field = LoyaltyOrdersFields
  type AggrResult = LoyaltyOrdersAggregate

  protected val aggrCsvConverter = loyaltyOrdersAggregateConverter

  val fieldsEnum = LoyaltyOrdersFields

  val endpoint = "loyalty_orders"

  override def tableName(filters: ReportFilters) =
    s"reports_loyalty_orders_func('${filters.merchantId}')"
  override lazy val tableNameAlias = "reports_loyalty_orders"

  override lazy val idColumn = s"$tableNameAlias.merchant_id"

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        data <- Option(LoyaltyOrdersAggregate.getResultOrZero(count, r))
        key <- Option(r.nextStringOption())
      } yield ReportFields(values = data, key = key),
    )

  def expandView(filters: ReportFilters): Option[String] = None

  def locationClauses(locationIds: Seq[UUID]) = Seq.empty

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.received_at_tz BETWEEN $from AND $to")

  def defaultJoins(filters: ReportFilters): Seq[String] = {
    val clauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      locationClauses(filters.locationIds)
    Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${clauses.mkString(" AND ")}")
  }
}
