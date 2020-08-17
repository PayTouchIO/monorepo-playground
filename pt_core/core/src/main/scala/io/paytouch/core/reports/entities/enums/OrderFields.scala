package io.paytouch.core.reports.entities.enums

import io.paytouch.core.reports.entities.enums.ops.{ Fields, FieldsEnum, ToIgnore }

sealed abstract class OrderFields(val columnName: String) extends FieldsEnum

case object OrderFields extends Fields[OrderFields] {

  case object Count extends OrderFields("ignore_me") with ToIgnore

  case object Revenue extends OrderFields("gross_sale_amount")

  case object Profit extends OrderFields("gross_profit_amount")

  case object WaitingTime extends OrderFields("waiting_time")

  val values = findValues

}
