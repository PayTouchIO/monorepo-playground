package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ ExternalColumnRef, Fields, FieldsEnum, ToIgnore }
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class LocationSalesFields(val columnName: String) extends FieldsEnum

case object LocationSalesFields extends Fields[LocationSalesFields] {

  case object Id extends LocationSalesFields("id") with ExternalColumnRef {
    lazy val tableRef = "locations"
    override def selector(filters: ReportFilters) = Some(s"$tableRef.id")

    override def groupBy = Some(s"$tableRef.id")

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Name extends LocationSalesFields("name") with ExternalColumnRef {
    lazy val tableRef = "locations"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Address extends LocationSalesFields("address_line_1") with ExternalColumnRef {
    lazy val tableRef = "locations"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Count extends LocationSalesFields("ignore_me") with ToIgnore

  case object GiftCardSales extends LocationSalesFields("gift_card_value_sales_amount")

  case object GrossProfits extends LocationSalesFields("gross_profit_amount")

  case object GrossSales extends LocationSalesFields("gross_sale_amount")

  case object NetSales extends LocationSalesFields("net_sale_amount")

  case object NonTaxable extends LocationSalesFields("non_taxable_amount")

  case object Taxable extends LocationSalesFields("taxable_amount")

  case object Costs extends LocationSalesFields("cogs_amount")

  case object Discounts extends LocationSalesFields("total_discount_amount")

  case object Refunds extends LocationSalesFields("refunds_amount")

  case object Taxes extends LocationSalesFields("collected_tax_amount")

  case object Tips extends LocationSalesFields("tip_amount")

  case object TenderTypes extends LocationSalesFields("ignore_me") with ToIgnore

  val values = findValues

  override val alwaysExpanded = Seq(Name, Address, Id)
}
