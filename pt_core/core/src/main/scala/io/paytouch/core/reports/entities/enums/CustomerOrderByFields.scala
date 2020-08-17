package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ AggregateInSelector, DescOrdering }
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class CustomerOrderByFields(val columnName: String) extends OrderByFieldsEnum

case object CustomerOrderByFields extends OrderByFields[CustomerOrderByFields] {

  case object FirstName extends CustomerOrderByFields("first_name")

  case object LastName extends CustomerOrderByFields("last_name")

  case object Id extends CustomerOrderByFields("id") {
    override val groupByColumn = "reports_customers.id"
  }

  case object Profit extends CustomerOrderByFields("profit") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = Some(s"COALESCE(SUM(reports_orders.gross_profit_amount), 0)")
  }

  case object Spend extends CustomerOrderByFields("revenue") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = Some(s"COALESCE(SUM(reports_orders.gross_sale_amount), 0)")
  }

  case object Visit extends CustomerOrderByFields("total_visit") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = Some(s"COUNT(reports_orders.id)")
  }

  case object Margin extends CustomerOrderByFields("total_margin") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) =
      Spend.selector(filters).map { spend =>
        s"""CASE $spend
           |      WHEN 0 THEN 0
           |      ELSE ROUND((1 - (COALESCE(SUM(reports_orders.cogs_amount), 0) / $spend))::numeric * 100, 4)
           | END""".stripMargin
      }
  }

  val values = findValues
}
