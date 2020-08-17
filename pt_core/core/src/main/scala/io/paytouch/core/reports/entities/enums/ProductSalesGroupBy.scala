package io.paytouch.core.reports.entities.enums

import enumeratum.Enum
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum

sealed abstract class ProductSalesGroupBy(val columnName: String) extends GroupByEnum

case object ProductSalesGroupBy extends Enum[ProductSalesGroupBy] {

  val values = findValues
}
