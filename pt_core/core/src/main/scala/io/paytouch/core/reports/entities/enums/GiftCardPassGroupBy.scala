package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum

sealed abstract class GiftCardPassGroupBy(val columnName: String) extends GroupByEnum

case object GiftCardPassGroupBy extends Enum[GiftCardPassGroupBy] {

  case object Value extends GiftCardPassGroupBy("original_balance_group")

  val values = findValues
}
