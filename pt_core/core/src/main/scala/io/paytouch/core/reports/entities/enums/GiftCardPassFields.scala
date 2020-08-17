package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.IntervalHelpers

sealed abstract class GiftCardPassFields(val columnName: String) extends FieldsEnum

case object GiftCardPassFields extends Fields[GiftCardPassFields] with IntervalHelpers {

  case object Count extends GiftCardPassFields("cnt") with ToIgnore

  case object Customers extends GiftCardPassFields("customers") {

    override def selector(filters: ReportFilters) = Some(s"COUNT(DISTINCT ${filters.view.tableNameAlias}.customer_id)")
  }

  case object Total extends GiftCardPassFields("remaining_amount") with SumOperation

  case object Redeemed extends GiftCardPassFields("redeemed_amount") {
    override def selector(filters: ReportFilters) = Some(s"COALESCE(SUM(- charges), 0)")
  }

  case object Unused extends GiftCardPassFields("unused_amount") {
    override def selector(filters: ReportFilters) =
      Total.selector(filters).map(total => s"$total + COALESCE(SUM(charges), 0)")
  }

  val values = findValues
}
