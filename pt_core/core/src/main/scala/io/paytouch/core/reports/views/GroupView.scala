package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.GroupTop
import io.paytouch.core.reports.entities.enums.ops.GroupOrderByFields
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object GroupView extends GroupView
trait GroupView extends ReportTopView {

  type OrderBy = GroupOrderByFields
  type TopResult = GroupTop

  val topCSVConverter = groupTopConverter

  val orderByEnum = GroupOrderByFields
  val endpoint = "groups"

  def locationClauses(locationIds: Seq[UUID]): Seq[String] = Seq.empty

  def dateClauses(from: String, to: String): Seq[String] = Seq.empty

  def topResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        id <- r.nextStringOption().map(UUID.fromString)
        margin <- r.nextBigDecimalOption()
        name <- r.nextStringOption()
        profit <- MonetaryAmount.extract(r.nextBigDecimalOption())
        spend <- MonetaryAmount.extract(r.nextBigDecimalOption())
        visits <- r.nextIntOption()
      } yield GroupTop(id = id, name = name, profit = profit, spend = spend, margin = margin, visits = visits),
    )

  def defaultJoins(filters: ReportFilters): Seq[String] =
    CustomerView.defaultJoins(filters) ++
      Seq(
        s"""LEFT OUTER JOIN customer_groups
           |ON customer_groups.customer_id = reports_customers.customer_id
           |AND customer_groups.created_at BETWEEN $intervalStartTimeSelector AND $intervalEndTimeSelector""".stripMargin,
        s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON customer_groups.group_id = $tableNameAlias.id",
      )
}
