package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ProductOrderByFields.Commons
import io.paytouch.core.reports.entities.enums.ops._
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class ProductSalesFields(val columnName: String) extends ListOrderByFieldsEnum

case object ProductSalesFields extends ListOrderByFields[ProductSalesFields] {

  case object Id extends ProductSalesFields("id") with ExternalColumnRef {
    lazy val tableRef = "products"
    override def selector(filters: ReportFilters) = Some(s"$tableRef.id")

    override def groupBy = Some(s"$tableRef.id")

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Name extends ProductSalesFields("name") with ExternalColumnRef {
    lazy val tableRef = "products"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Sku extends ProductSalesFields("sku") with ExternalColumnRef {
    lazy val tableRef = "products"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Upc extends ProductSalesFields("upc") with ExternalColumnRef {
    lazy val tableRef = "products"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object DeletedAt extends ProductSalesFields("deleted_at") with ExternalColumnRef {
    lazy val tableRef = "products"

    override def aggregatedSelector(op: String, table: String) = s"$columnName"
  }

  case object Cost extends ProductSalesFields("cogs_amount") with SumOperation with DescOrdering

  case object Count extends ProductSalesFields("ignore_me") with ToIgnore

  case object Discounts extends ProductSalesFields("discount_amount") with SumOperation with DescOrdering

  case object GrossProfits extends ProductSalesFields("gross_profit_amount") with SumOperation with DescOrdering

  case object GrossSales extends ProductSalesFields("gross_sale_amount") with SumOperation with DescOrdering

  case object Margin extends ProductSalesFields("margin") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = {
      val netSales = "COALESCE(SUM(reports_product_sales.net_sale_amount), 0)"
      val cost = "COALESCE(SUM(reports_product_sales.cogs_amount), 0)"
      Some(s"""CASE $netSales
              |      WHEN 0 THEN 0
              |      ELSE ROUND((1 - ($cost / $netSales)) * 100, 4)
              |   END""".stripMargin)
    }
  }

  case object NetSales extends ProductSalesFields("net_sale_amount") with SumOperation with DescOrdering

  case object Quantity extends ProductSalesFields("quantity") with SumOperation with DescOrdering

  case object ReturnedQuantity extends ProductSalesFields("returned_quantity") with SumOperation with DescOrdering

  case object ReturnedAmount extends ProductSalesFields("returned_amount") with SumOperation with DescOrdering

  case object Taxes extends ProductSalesFields("tax_amount") with SumOperation with DescOrdering

  case object Taxable extends ProductSalesFields("taxable_amount") with SumOperation with DescOrdering

  case object NonTaxable extends ProductSalesFields("non_taxable_amount") with SumOperation with DescOrdering

  val values = findValues

  val defaultOrdering = Seq(Name)

  override val alwaysExpanded = Seq(Name, Sku, Upc, Id, DeletedAt)

  object Commons {}
}
