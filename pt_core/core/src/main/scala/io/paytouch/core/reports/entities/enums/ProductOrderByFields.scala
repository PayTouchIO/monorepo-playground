package io.paytouch.core.reports.entities.enums
import io.paytouch.core.reports.entities.enums.ops.{ AggregateInSelector, DescOrdering }
import io.paytouch.core.reports.filters.ReportFilters

sealed abstract class ProductOrderByFields(val columnName: String) extends OrderByFieldsEnum

case object ProductOrderByFields extends OrderByFields[ProductOrderByFields] {

  case object Id extends ProductOrderByFields("id") {
    override def selector(filters: ReportFilters) = Some(s"products.id")
    override val groupBy = Some("products.id")
  }

  case object Name extends ProductOrderByFields("name") {
    override def selector(filters: ReportFilters) = Some(s"products.name")
    override val groupBy = Some("products.name")
  }

  case object NetSales extends ProductOrderByFields("net_sales") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = Some(s"${Commons.NetSales}")
  }

  case object Quantity extends ProductOrderByFields("total_quantity") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = Some(s"SUM(order_items.quantity)")
  }

  case object Revenue extends ProductOrderByFields("total_revenue") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = Some(s"${Commons.GrossSales}")
  }

  case object Profit extends ProductOrderByFields("total_profit") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = {
      val netSales = Commons.NetSales
      val cost = Commons.Cost
      Some(s"($netSales - $cost)")
    }
  }

  case object Margin extends ProductOrderByFields("total_margin") with AggregateInSelector with DescOrdering {
    override def selector(filters: ReportFilters) = {
      val netSales = Commons.NetSales
      val cost = Commons.Cost
      Some(s"""CASE $netSales
              |      WHEN 0 THEN 0
              |      ELSE ROUND((1 - ($cost / $netSales)) * 100, 4)
              |   END""".stripMargin)
    }
  }

  val values = findValues

  object Commons {

    val GrossSales = defaultedToZero("SUM(order_items.total_price_amount)")
    val NetSales = defaultedToZero("SUM(order_items.total_price_amount - COALESCE(order_items.tax_amount, 0))")
    val Cost = defaultedToZero("SUM(order_items.quantity * order_items.cost_amount)")

    private def defaultedToZero(s: String) = s"COALESCE($s, 0)"
  }
}
