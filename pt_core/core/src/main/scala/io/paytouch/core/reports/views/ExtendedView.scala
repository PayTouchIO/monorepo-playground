package io.paytouch.core.reports.views

import io.paytouch.core.reports.entities.enums.ops.ListOrderByFields
import io.paytouch.core.reports.filters.ReportFilters

trait ExtendedView[V <: ReportListView] {
  val listTable: String
  def listWhereClauses(filters: ReportFilters): Seq[String]
  val orderByEnum: ListOrderByFields[V#OrderBy]

  def expandView(filters: ReportFilters) = {
    val selectors = orderByEnum.alwaysExpanded.map(_.columnName)
    val query = s"""SELECT ${selectors.mkString(", ")}
                   |FROM $listTable
                   |WHERE ${listWhereClauses(filters).mkString(" AND ")}""".stripMargin
    Some(query)
  }

}
