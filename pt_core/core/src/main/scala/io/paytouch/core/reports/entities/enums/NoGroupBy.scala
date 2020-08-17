package io.paytouch.core.reports.entities.enums

import enumeratum.Enum
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum

sealed abstract class NoGroupBy(val columnName: String) extends GroupByEnum

case object NoGroupBy extends Enum[NoGroupBy] {

  val values = findValues
}
