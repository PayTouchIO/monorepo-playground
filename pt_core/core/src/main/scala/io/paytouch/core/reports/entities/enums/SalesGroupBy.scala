package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum

sealed abstract class SalesGroupBy(val columnName: String) extends GroupByEnum

case object SalesGroupBy extends Enum[SalesGroupBy] {

  val values = findValues
}
