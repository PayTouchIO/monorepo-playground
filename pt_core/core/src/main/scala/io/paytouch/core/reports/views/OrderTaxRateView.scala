package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums.{ OrderTaxRateFields, OrderTaxRateGroupBy }
import io.paytouch.core.reports.entities.{ OrderTaxRateAggregate, ReportFields }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object OrderTaxRateView extends OrderTaxRateView
trait OrderTaxRateView extends ReportAggrView {

  type GroupBy = OrderTaxRateGroupBy
  type Field = OrderTaxRateFields
  type AggrResult = OrderTaxRateAggregate

  protected val aggrCsvConverter = orderTaxRateAggregateConverter

  val fieldsEnum = OrderTaxRateFields

  val endpoint = "order_tax_rates"

  override def tableName(filters: ReportFilters) = s"reports_order_tax_rates_func('${filters.merchantId}')"
  override lazy val tableNameAlias = "reports_order_tax_rates"
  override lazy val idColumn = s"$tableNameAlias.merchant_id"
  override val countSelectors = Seq(s"COUNT($tableNameAlias.id) AS cnt")
  override val baseGroupBys = Seq.empty

  def expandView(filters: ReportFilters) = None

  def locationClauses(locationIds: Seq[UUID]): Seq[String] = Seq.empty

  def dateClauses(from: String, to: String): Seq[String] = Seq.empty

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        amount <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
        key <- Option(r.nextStringOption())
      } yield ReportFields(values = OrderTaxRateAggregate(count, amount), key = key),
    )

  def defaultJoins(filters: ReportFilters): Seq[String] =
    OrderView.defaultJoins(filters) ++
      Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON reports_orders.id = $tableNameAlias.order_id")

}
