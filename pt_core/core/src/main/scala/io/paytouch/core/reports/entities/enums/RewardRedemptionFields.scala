package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.IntervalHelpers

sealed abstract class RewardRedemptionsFields(val columnName: String) extends FieldsEnum

case object RewardRedemptionsFields extends Fields[RewardRedemptionsFields] with IntervalHelpers {

  case object Count extends RewardRedemptionsFields("cnt") with ToIgnore

  case object Value extends RewardRedemptionsFields("value") with SumOperation

  val values = findValues
}
