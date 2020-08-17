package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.reports.entities.enums.ops.{ AggregateInSelector, GroupByEnum }
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.IntervalHelpers
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class CustomerGroupBy(val columnName: String) extends GroupByEnum

case object CustomerGroupBy extends Enum[CustomerGroupBy] with IntervalHelpers {

  case object Type extends CustomerGroupBy("customer_type") {
    val `new` = CustomerType.New.entryName
    val returning = CustomerType.Returning.entryName

    override def selector(filters: ReportFilters) =
      Some(
        s"""(SELECT CASE reports_customers.first_order_received_at < $intervalStartTimeSelector WHEN true THEN '$returning'  ELSE '${`new`}' END)""",
      )
  }

  case object Visit extends CustomerGroupBy("total_visits") with AggregateInSelector {
    override def selector(filters: ReportFilters) = Some(s"COUNT(reports_orders.id)")
  }

  case object LoyaltyProgram extends CustomerGroupBy("loyalty_program") {
    val `with` = LoyaltyProgramStatus.With.entryName
    val without = LoyaltyProgramStatus.Without.entryName

    override def selector(filters: ReportFilters) =
      Some(
        s"""(SELECT CASE reports_customers.first_loyalty_opt_in_at < $intervalStartTimeSelector WHEN true THEN '${`with`}'  ELSE '$without' END)""",
      )
  }

  val values = findValues
}

sealed trait CustomerType extends EnumEntrySnake

case object CustomerType extends Enum[CustomerType] {

  case object New extends CustomerType
  case object Returning extends CustomerType

  val values = findValues
}

sealed trait LoyaltyProgramStatus extends EnumEntrySnake

case object LoyaltyProgramStatus extends Enum[LoyaltyProgramStatus] {

  case object With extends LoyaltyProgramStatus
  case object Without extends LoyaltyProgramStatus

  val values = findValues
}
