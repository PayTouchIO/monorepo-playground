package io.paytouch.core.reports.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait CsvExports extends EnumEntrySnake

case object CsvExports extends Enum[CsvExports] {

  case object Customers extends CsvExports
  case object InventoryCount extends CsvExports
  case object CashDrawers extends CsvExports
  case object ReimportableProducts extends CsvExports

  val values = findValues
}
