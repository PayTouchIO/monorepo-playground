package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ OrderAggregate, ReportFields }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object OrderView extends OrderView

trait OrderView extends ReportAggrView {

  type GroupBy = OrderGroupBy
  type Field = OrderFields
  type AggrResult = OrderAggregate

  protected val aggrCsvConverter = orderAggregateConverter

  val fieldsEnum = OrderFields
  lazy val endpoint = "orders"

  override def tableName(filters: ReportFilters) = tableNameAlias
  override lazy val tableNameAlias = "reports_orders"
  override lazy val idColumn = s"$tableNameAlias.merchant_id"

  def expandView(filters: ReportFilters) = None

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        profit <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
        revenue <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
        waitingTimeInSeconds <- Option(r.nextIntOption())
        key <- Option(r.nextStringOption())
      } yield ReportFields(
        values = OrderAggregate(
          count = count,
          profit = profit,
          revenue = revenue,
          waitingTimeInSeconds = waitingTimeInSeconds,
        ),
        key = key,
      ),
    )

  def locationClauses(locationIds: Seq[UUID]): Seq[String] =
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.received_at_tz BETWEEN $from AND $to")

  def defaultClauses(filters: ReportFilters, withVoided: Boolean = false): Seq[String] = {
    val paymentStatusesCond =
      if (withVoided) "!= 'pending'"
      else s"IN (${PaymentStatus.isPaid.map(_.entryName).asInParametersList})"

    val orderTypesFilter = filters
      .orderTypes
      .fold[Seq[String]](Seq.empty)(ot => Seq(s"$tableNameAlias.type IN (${ot.map(_.entryName).asInParametersList})"))

    Seq(
      s"$tableNameAlias.is_invoice = false",
      s"$tableNameAlias.payment_status $paymentStatusesCond",
    ) ++ orderTypesFilter
  }

  def defaultJoins(filters: ReportFilters) = defaultJoins(filters, withVoided = false)

  def defaultJoins(filters: ReportFilters, withVoided: Boolean) = {
    val clauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      locationClauses(filters.locationIds) ++ defaultClauses(filters, withVoided)
    Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${clauses.mkString(" AND ")}")
  }

  val allNonPending = Seq(s"AND $tableNameAlias.payment_status != 'pending'")

}
