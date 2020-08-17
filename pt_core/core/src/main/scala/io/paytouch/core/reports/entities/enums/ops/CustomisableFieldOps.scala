package io.paytouch.core.reports.entities.enums.ops

import io.paytouch.core.reports.filters.ReportFilters

trait SumOperation extends AggregateInSelector { self: CustomisableField =>

  override def selector(filters: ReportFilters): Option[String] =
    Some(s"COALESCE(SUM(${filters.view.tableNameAlias}.$columnName), 0)")

}

trait AggregateInSelector { self: CustomisableField =>

  override def aggregateInSelector = true
}

trait ExternalColumnRef { self: CustomisableField =>
  override def selector(filters: ReportFilters) = Some(groupByColumn)
  override def groupByColumn = s"$tableRef.$columnName"
  def tableRef: String
}

trait ToIgnore { self: CustomisableField =>

  override def toIgnore = true
}
