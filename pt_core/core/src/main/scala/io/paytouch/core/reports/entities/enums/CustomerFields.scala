package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ AggregateInSelector, Fields, FieldsEnum, ToIgnore }
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.IntervalHelpers

sealed abstract class CustomerFields(val columnName: String) extends FieldsEnum

case object CustomerFields extends Fields[CustomerFields] with IntervalHelpers {

  case object Count extends CustomerFields("cnt") with ToIgnore

  case object Spend extends CustomerFields("total_price") with AggregateInSelector {
    override def selector(filters: ReportFilters) = Some(s"COALESCE(SUM(reports_orders.gross_sale_amount), 0)")
  }

  val values = findValues
}
