package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class LocationGiftCardPassesOrderByFields(override val columnName: String) extends ListOrderByFieldsEnum

case object LocationGiftCardPassesOrderByFields extends ListOrderByFields[LocationGiftCardPassesOrderByFields] {

  case object Id extends LocationGiftCardPassesOrderByFields("id") with ExternalColumnRef {
    override def groupByColumn = "reports_orders.location_id"
    lazy val tableRef = "reports_orders"
  }

  case object Name extends LocationGiftCardPassesOrderByFields("name") with ExternalColumnRef {
    lazy val tableRef = "locations"
  }

  case object Address extends LocationGiftCardPassesOrderByFields("address_line_1") with ExternalColumnRef {
    lazy val tableRef = "locations"
  }

  case object Count extends LocationGiftCardPassesOrderByFields("cnt") with ToIgnore with DescOrdering

  case object Customers extends LocationGiftCardPassesOrderByFields("customers") with SumOperation with DescOrdering {
    override def selector(filters: ReportFilters) = LocationGiftCardPassFields.Customers.selector(filters)
  }

  case object Total extends LocationGiftCardPassesOrderByFields("remaining_amount") with SumOperation with DescOrdering

  case object Redeemed
      extends LocationGiftCardPassesOrderByFields("redeemed_amount")
         with SumOperation
         with DescOrdering {
    override def selector(filters: ReportFilters) = LocationGiftCardPassFields.Redeemed.selector(filters)
  }

  case object Unused extends LocationGiftCardPassesOrderByFields("unused_amount") with SumOperation with DescOrdering {
    override def selector(filters: ReportFilters) = LocationGiftCardPassFields.Unused.selector(filters)
  }

  val values = findValues

  val defaultOrdering = Seq(Name, Address)

  override val alwaysExpanded = Seq(Address, Id, Name)
}
