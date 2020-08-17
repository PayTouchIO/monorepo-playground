package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ Fields, FieldsEnum, ToIgnore }

sealed abstract class SalesFields(val columnName: String) extends FieldsEnum

case object SalesFields extends Fields[SalesFields] {

  case object Costs extends SalesFields("cogs_amount")

  case object Count extends SalesFields("ignore_me") with ToIgnore

  case object Discounts extends SalesFields("total_discount_amount")

  case object GiftCardSales extends SalesFields("gift_card_value_sales_amount")

  case object GrossProfits extends SalesFields("gross_profit_amount")

  case object GrossSales extends SalesFields("gross_sale_amount")

  case object NetSales extends SalesFields("net_sale_amount")

  case object NonTaxable extends SalesFields("non_taxable_amount")

  case object Refunds extends SalesFields("refunds_amount")

  case object Taxable extends SalesFields("taxable_amount")

  case object Taxes extends SalesFields("collected_tax_amount")

  case object TenderTypes extends SalesFields("ignore_me") with ToIgnore

  case object Tips extends SalesFields("tip_amount")

  val values = findValues

}
