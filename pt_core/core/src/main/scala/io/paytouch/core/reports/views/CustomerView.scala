package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums.{ CustomerFields, CustomerGroupBy, CustomerOrderByFields }
import io.paytouch.core.reports.entities.{ CustomerAggregate, CustomerTop, ReportFields }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object CustomerView extends CustomerView
trait CustomerView extends ReportAggrView with ReportTopView {

  type GroupBy = CustomerGroupBy
  type Field = CustomerFields
  type AggrResult = CustomerAggregate

  type OrderBy = CustomerOrderByFields
  type TopResult = CustomerTop

  protected val aggrCsvConverter = customerAggregateConverter
  val topCSVConverter = customerTopConverter

  val fieldsEnum = CustomerFields
  val orderByEnum = CustomerOrderByFields

  val endpoint = "customers"

  override def tableName(filters: ReportFilters) =
    s"reports_customers_func('${filters.merchantId}', '{${filters.locationIds.mkString(",")}}')"
  override lazy val tableNameAlias = "reports_customers"
  override lazy val idColumn = s"$tableNameAlias.customer_id"

  override val groupByInOuterQuery = true

  def expandView(filters: ReportFilters) = None

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        spend <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
        key <- Option(r.nextStringOption())
      } yield ReportFields(values = CustomerAggregate(count, spend), key = key),
    )

  def topResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        firstName <- Option(r.nextStringOption())
        id <- r.nextStringOption().map(UUID.fromString)
        lastName <- Option(r.nextStringOption())
        margin <- r.nextBigDecimalOption()
        profit <- MonetaryAmount.extract(r.nextBigDecimalOption())
        spend <- MonetaryAmount.extract(r.nextBigDecimalOption())
        visits <- r.nextIntOption()
      } yield CustomerTop(
        firstName = firstName,
        id = id,
        lastName = lastName,
        profit = profit,
        spend = spend,
        margin = margin,
        visits = visits,
      ),
    )

  def locationClauses(locationIds: Seq[UUID]) =
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = Seq.empty

  def defaultJoins(filters: ReportFilters): Seq[String] = {
    val onClauses = Seq(
      s"reports_orders.customer_id = $tableNameAlias.customer_id",
      s"reports_orders.location_id = $tableNameAlias.location_id",
    ) ++ locationClauses(filters.locationIds)

    OrderView.defaultJoins(filters) ++
      Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${onClauses.mkString(" AND ")}")
  }
}
