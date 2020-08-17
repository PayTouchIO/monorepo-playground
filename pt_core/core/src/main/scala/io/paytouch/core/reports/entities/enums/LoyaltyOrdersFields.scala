package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.IntervalHelpers

sealed abstract class LoyaltyOrdersFields(val columnName: String) extends FieldsEnum

case object LoyaltyOrdersFields extends Fields[LoyaltyOrdersFields] with IntervalHelpers {

  case object Count extends LoyaltyOrdersFields("cnt") with ToIgnore

  case object Amount extends LoyaltyOrdersFields("order_total_amount") with SumOperation

  val values = findValues
}
