package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class CategorySalesOrderByFields(val columnName: String) extends ListOrderByFieldsEnum

case object CategorySalesOrderByFields extends ListOrderByFields[CategorySalesOrderByFields] {

  case object Id extends CategorySalesOrderByFields("id") with ExternalColumnRef {
    lazy val tableRef = "categories"
  }

  case object Name extends CategorySalesOrderByFields("name") with ExternalColumnRef {
    lazy val tableRef = "categories"
  }

  case object Count extends CategorySalesOrderByFields("cnt") with ToIgnore with DescOrdering

  case object Cost extends CategorySalesOrderByFields("cogs_amount") with SumOperation with DescOrdering

  case object Discounts extends CategorySalesOrderByFields("discount_amount") with SumOperation with DescOrdering

  case object GrossProfits extends CategorySalesOrderByFields("gross_profit_amount") with SumOperation with DescOrdering

  case object GrossSales extends CategorySalesOrderByFields("gross_sale_amount") with SumOperation with DescOrdering

  case object Margin extends CategorySalesOrderByFields("margin") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = {
      val netSales = "COALESCE(SUM(reports_product_sales.net_sale_amount), 0)"
      val cost = "COALESCE(SUM(reports_product_sales.cogs_amount), 0)"
      Some(s"""CASE $netSales
              |      WHEN 0 THEN 0
              |      ELSE ROUND((1 - ($cost / $netSales)) * 100, 4)
              |   END""".stripMargin)
    }
  }
  case object NetSales extends CategorySalesOrderByFields("net_sale_amount") with SumOperation with DescOrdering

  case object Quantity extends CategorySalesOrderByFields("quantity") with SumOperation with DescOrdering

  case object ReturnedQuantity
      extends CategorySalesOrderByFields("returned_quantity")
         with SumOperation
         with DescOrdering

  case object ReturnedAmount extends CategorySalesOrderByFields("returned_amount") with SumOperation with DescOrdering

  case object Taxes extends CategorySalesOrderByFields("tax_amount") with SumOperation with DescOrdering

  case object Taxable extends CategorySalesOrderByFields("taxable_amount") with SumOperation with DescOrdering

  case object NonTaxable extends CategorySalesOrderByFields("non_taxable_amount") with SumOperation with DescOrdering

  val values = findValues

  val defaultOrdering = Seq(Name)

  override val alwaysExpanded = Seq(Id, Name)
}
