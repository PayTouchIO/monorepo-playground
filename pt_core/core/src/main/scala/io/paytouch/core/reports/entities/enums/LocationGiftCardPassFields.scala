package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class LocationGiftCardPassFields(val columnName: String) extends FieldsEnum

case object LocationGiftCardPassFields extends Fields[LocationGiftCardPassFields] {

  case object Id extends LocationGiftCardPassFields("id") with ExternalColumnRef {
    lazy val tableRef = "locations"
    override def selector(filters: ReportFilters) = Some(s"$tableRef.id")

    override def groupBy = Some(s"$tableRef.id")

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Name extends LocationGiftCardPassFields("name") with ExternalColumnRef {
    lazy val tableRef = "locations"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Address extends LocationGiftCardPassFields("address_line_1") with ExternalColumnRef {
    lazy val tableRef = "locations"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Count extends LocationGiftCardPassFields("ignore_me") with ToIgnore

  case object Customers extends LocationGiftCardPassFields("customers") {

    override def selector(filters: ReportFilters) = GiftCardPassFields.Customers.selector(filters)
  }

  case object Total extends LocationGiftCardPassFields("remaining_amount") with SumOperation

  case object Redeemed extends LocationGiftCardPassFields("redeemed_amount") {
    override def selector(filters: ReportFilters) = GiftCardPassFields.Redeemed.selector(filters)
  }

  case object Unused extends LocationGiftCardPassFields("unused_amount") {
    override def selector(filters: ReportFilters) = GiftCardPassFields.Unused.selector(filters)
  }

  val values = findValues

  override val alwaysExpanded = Seq(Name, Address, Id)
}
