package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum

sealed abstract class OrderTaxRateGroupBy(val columnName: String) extends GroupByEnum

case object OrderTaxRateGroupBy extends Enum[OrderTaxRateGroupBy] {

  case object Name extends OrderTaxRateGroupBy("name")

  val values = findValues
}
