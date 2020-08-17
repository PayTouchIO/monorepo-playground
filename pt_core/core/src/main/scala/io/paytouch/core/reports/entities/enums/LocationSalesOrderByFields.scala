package io.paytouch.core.reports.entities.enums
import io.paytouch.core.reports.entities.enums.ops._

sealed abstract class LocationSalesOrderByFields(override val columnName: String) extends ListOrderByFieldsEnum

case object LocationSalesOrderByFields extends ListOrderByFields[LocationSalesOrderByFields] {

  case object Id extends LocationSalesOrderByFields("id") with ExternalColumnRef {
    override def groupByColumn = "reports_orders.location_id"
    lazy val tableRef = "reports_orders"
  }

  case object Name extends LocationSalesOrderByFields("name") with ExternalColumnRef {
    lazy val tableRef = "locations"
  }

  case object Address extends LocationSalesOrderByFields("address_line_1") with ExternalColumnRef {
    lazy val tableRef = "locations"
  }

  case object Costs extends LocationSalesOrderByFields("cogs_amount") with SumOperation with DescOrdering

  case object Count extends LocationSalesOrderByFields("cnt") with ToIgnore with DescOrdering

  case object Discounts extends LocationSalesOrderByFields("total_discount_amount") with SumOperation with DescOrdering

  case object GiftCardSales
      extends LocationSalesOrderByFields("gift_card_value_sales_amount")
         with SumOperation
         with DescOrdering

  case object GrossProfits extends LocationSalesOrderByFields("gross_profit_amount") with SumOperation with DescOrdering

  case object GrossSales extends LocationSalesOrderByFields("gross_sale_amount") with SumOperation with DescOrdering

  case object NetSales extends LocationSalesOrderByFields("net_sale_amount") with SumOperation with DescOrdering

  case object NonTaxable extends LocationSalesOrderByFields("non_taxable_amount") with SumOperation with DescOrdering

  case object Refunds extends LocationSalesOrderByFields("refunds_amount") with SumOperation with DescOrdering

  case object Taxable extends LocationSalesOrderByFields("taxable_amount") with SumOperation with DescOrdering

  case object Taxes extends LocationSalesOrderByFields("collected_tax_amount") with SumOperation with DescOrdering

  case object Tips extends LocationSalesOrderByFields("tip_amount") with SumOperation with DescOrdering

  case object TenderTypes extends LocationSalesOrderByFields("ignore_me") with ToIgnore

  val values = findValues

  val defaultOrdering = Seq(Name, Address)

  override val alwaysExpanded = Seq(Address, Id, Name)
}
