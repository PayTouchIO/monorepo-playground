package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class ProductSalesOrderByFields(val columnName: String) extends ListOrderByFieldsEnum

case object ProductSalesOrderByFields extends ListOrderByFields[ProductSalesOrderByFields] {

  case object Id extends ProductSalesOrderByFields("id") with ExternalColumnRef {
    lazy val tableRef = "products"
  }

  case object Name extends ProductSalesOrderByFields("name") with ExternalColumnRef {
    lazy val tableRef = "products"
  }

  case object Sku extends ProductSalesOrderByFields("sku") with ExternalColumnRef {
    lazy val tableRef = "products"
  }

  case object Upc extends ProductSalesOrderByFields("upc") with ExternalColumnRef {
    lazy val tableRef = "products"
  }

  case object DeletedAt extends ProductSalesOrderByFields("deleted_at") with ExternalColumnRef {
    lazy val tableRef = "products"
  }

  case object Count extends ProductSalesOrderByFields("cnt") with ToIgnore with DescOrdering

  case object Cost extends ProductSalesOrderByFields("cogs_amount") with SumOperation with DescOrdering

  case object Discounts extends ProductSalesOrderByFields("discount_amount") with SumOperation with DescOrdering

  case object GrossProfits extends ProductSalesOrderByFields("gross_profit_amount") with SumOperation with DescOrdering

  case object GrossSales extends ProductSalesOrderByFields("gross_sale_amount") with SumOperation with DescOrdering

  case object Margin extends ProductSalesOrderByFields("margin") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = {
      val netSales = "COALESCE(SUM(reports_product_sales.net_sale_amount), 0)"
      val cost = "COALESCE(SUM(reports_product_sales.cogs_amount), 0)"
      Some(s"""CASE $netSales
              |      WHEN 0 THEN 0
              |      ELSE ROUND((1 - ($cost / $netSales)) * 100, 4)
              |   END""".stripMargin)
    }
  }

  case object NetSales extends ProductSalesOrderByFields("net_sale_amount") with SumOperation with DescOrdering

  case object Quantity extends ProductSalesOrderByFields("quantity") with SumOperation with DescOrdering

  case object ReturnedQuantity
      extends ProductSalesOrderByFields("returned_quantity")
         with SumOperation
         with DescOrdering

  case object ReturnedAmount extends ProductSalesOrderByFields("returned_amount") with SumOperation with DescOrdering

  case object Taxes extends ProductSalesOrderByFields("tax_amount") with SumOperation with DescOrdering

  case object Taxable extends ProductSalesOrderByFields("taxable_amount") with SumOperation with DescOrdering

  case object NonTaxable extends ProductSalesOrderByFields("non_taxable_amount") with SumOperation with DescOrdering

  val values = findValues

  val defaultOrdering = Seq(Name)

  override val alwaysExpanded = Seq(Id, Name, Sku, Upc, DeletedAt)
}
