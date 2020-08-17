package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.reports.entities.enums.ops.GroupByEnum

sealed abstract class OrderGroupBy(val columnName: String) extends GroupByEnum

case object OrderGroupBy extends Enum[OrderGroupBy] {

  case object PaymentType extends OrderGroupBy("payment_type")

  case object OrderType extends OrderGroupBy("type")

  case object SourceType extends OrderGroupBy("source")

  case object Feedback extends OrderGroupBy("feedback_rating")

  val values = findValues
}
