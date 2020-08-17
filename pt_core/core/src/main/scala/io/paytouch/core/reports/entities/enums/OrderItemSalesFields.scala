package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ Fields, FieldsEnum, SumOperation, ToIgnore }

sealed abstract class OrderItemSalesFields(val columnName: String) extends FieldsEnum

case object OrderItemSalesFields extends Fields[OrderItemSalesFields] {

  case object Count extends OrderItemSalesFields("ignore_me") with ToIgnore

  case object Cost extends OrderItemSalesFields("cost_amount") with SumOperation

  case object Discounts extends OrderItemSalesFields("discount_amount") with SumOperation

  case object GrossProfits extends OrderItemSalesFields("gross_sale_amount") with SumOperation

  case object GrossSales extends OrderItemSalesFields("gross_sale_amount") with SumOperation

  case object Margin extends OrderItemSalesFields("margin") with SumOperation

  case object NetSales extends OrderItemSalesFields("net_sale_amount") with SumOperation

  case object Quantity extends OrderItemSalesFields("quantity") with SumOperation

  case object ReturnedQuantity extends OrderItemSalesFields("returned_quantity") with SumOperation

  case object ReturnedAmount extends OrderItemSalesFields("returned_amount") with SumOperation

  case object Taxes extends OrderItemSalesFields("taxes") with SumOperation

  case object Taxable extends OrderItemSalesFields("taxable_amount") with SumOperation

  case object NonTaxable extends OrderItemSalesFields("non_taxable_amount") with SumOperation

  val values = findValues

}
