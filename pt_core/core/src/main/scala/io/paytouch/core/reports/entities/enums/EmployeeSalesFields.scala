package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ ExternalColumnRef, Fields, FieldsEnum, ToIgnore }
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class EmployeeSalesFields(val columnName: String) extends FieldsEnum

case object EmployeeSalesFields extends Fields[EmployeeSalesFields] {

  case object Id extends EmployeeSalesFields("id") with ExternalColumnRef {
    lazy val tableRef = "users"
    override def selector(filters: ReportFilters) = Some(s"$tableRef.id")

    override def groupBy = Some(s"$tableRef.id")

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object FirstName extends EmployeeSalesFields("first_name") with ExternalColumnRef {
    lazy val tableRef = "users"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object LastName extends EmployeeSalesFields("last_name") with ExternalColumnRef {
    lazy val tableRef = "users"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Count extends EmployeeSalesFields("ignore_me") with ToIgnore

  case object GiftCardSales extends EmployeeSalesFields("gift_card_value_sales_amount")

  case object GrossProfits extends EmployeeSalesFields("gross_profit_amount")

  case object GrossSales extends EmployeeSalesFields("gross_sale_amount")

  case object NetSales extends EmployeeSalesFields("net_sale_amount")

  case object NonTaxable extends EmployeeSalesFields("non_taxable_amount")

  case object Taxable extends EmployeeSalesFields("taxable_amount")

  case object Costs extends EmployeeSalesFields("cogs_amount")

  case object Discounts extends EmployeeSalesFields("total_discount_amount")

  case object Refunds extends EmployeeSalesFields("refunds_amount")

  case object Taxes extends EmployeeSalesFields("collected_tax_amount")

  case object Tips extends EmployeeSalesFields("tip_amount")

  case object TenderTypes extends EmployeeSalesFields("ignore_me") with ToIgnore

  val values = findValues

  override val alwaysExpanded = Seq(Id, FirstName, LastName)
}
