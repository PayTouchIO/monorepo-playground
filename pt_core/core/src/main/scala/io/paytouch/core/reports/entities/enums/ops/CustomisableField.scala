package io.paytouch.core.reports.entities.enums.ops

import enumeratum.EnumEntry
import io.paytouch.core.reports.filters.ReportFilters

trait CustomisableField {
  def columnName: String

  def selector(filters: ReportFilters): Option[String] = None
  def aggregatedSelector(op: String, table: String) = s"COALESCE($op($table.$columnName), 0)"

  def groupBy: Option[String] = if (aggregateInSelector) None else Some(groupByColumn)
  def groupByColumn = columnName

  def aggregateInSelector = false
  def toIgnore = false
}

class CustomisableCopiedField(other: CustomisableField) extends EnumEntry with CustomisableField {
  val columnName = other.columnName
  override def selector(filters: ReportFilters): Option[String] = other.selector(filters)
  override def groupBy: Option[String] = other.groupBy
}
