package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class LoyaltyOrdersGroupBy(val columnName: String) extends GroupByEnum

case object LoyaltyOrdersGroupBy extends Enum[LoyaltyOrdersGroupBy] {

  case object Type extends LoyaltyOrdersGroupBy("type")

  val values = findValues
}
