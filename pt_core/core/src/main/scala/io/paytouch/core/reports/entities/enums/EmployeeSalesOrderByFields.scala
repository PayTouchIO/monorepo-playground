package io.paytouch.core.reports.entities.enums
import io.paytouch.core.reports.entities.enums.ops._

sealed abstract class EmployeeSalesOrderByFields(override val columnName: String) extends ListOrderByFieldsEnum

case object EmployeeSalesOrderByFields extends ListOrderByFields[EmployeeSalesOrderByFields] {

  case object Id extends EmployeeSalesOrderByFields("id") with ExternalColumnRef {
    override def groupByColumn = "reports_orders.employee_id"
    lazy val tableRef = "reports_orders"
  }

  case object FirstName extends EmployeeSalesOrderByFields("first_name") with ExternalColumnRef {
    lazy val tableRef = "locations"
  }

  case object LastName extends EmployeeSalesOrderByFields("last_name") with ExternalColumnRef {
    lazy val tableRef = "locations"
  }

  case object Costs extends EmployeeSalesOrderByFields("cogs_amount") with SumOperation with DescOrdering

  case object Count extends EmployeeSalesOrderByFields("cnt") with ToIgnore with DescOrdering

  case object Discounts extends EmployeeSalesOrderByFields("total_discount_amount") with SumOperation with DescOrdering

  case object GiftCardSales
      extends EmployeeSalesOrderByFields("gift_card_value_sales_amount")
         with SumOperation
         with DescOrdering

  case object GrossProfits extends EmployeeSalesOrderByFields("gross_profit_amount") with SumOperation with DescOrdering

  case object GrossSales extends EmployeeSalesOrderByFields("gross_sale_amount") with SumOperation with DescOrdering

  case object NetSales extends EmployeeSalesOrderByFields("net_sale_amount") with SumOperation with DescOrdering

  case object NonTaxable extends EmployeeSalesOrderByFields("non_taxable_amount") with SumOperation with DescOrdering

  case object Refunds extends EmployeeSalesOrderByFields("refunds_amount") with SumOperation with DescOrdering

  case object Taxable extends EmployeeSalesOrderByFields("taxable_amount") with SumOperation with DescOrdering

  case object Taxes extends EmployeeSalesOrderByFields("collected_tax_amount") with SumOperation with DescOrdering

  case object Tips extends EmployeeSalesOrderByFields("tip_amount") with SumOperation with DescOrdering

  val values = findValues

  val defaultOrdering = Seq(FirstName, LastName)

  override val alwaysExpanded = Seq(Id, FirstName, LastName)
}
