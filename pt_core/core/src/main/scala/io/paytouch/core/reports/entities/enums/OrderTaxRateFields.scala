package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops._

sealed abstract class OrderTaxRateFields(val columnName: String) extends FieldsEnum

case object OrderTaxRateFields extends Fields[OrderTaxRateFields] {

  case object Count extends OrderTaxRateFields("ignore_me") with ToIgnore

  case object Amount extends OrderTaxRateFields("total_amount")

  val values = findValues
}
