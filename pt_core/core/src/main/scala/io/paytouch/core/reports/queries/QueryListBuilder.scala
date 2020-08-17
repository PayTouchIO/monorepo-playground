package io.paytouch.core.reports.queries

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.filters.ReportListFilters
import io.paytouch.core.reports.views.ReportListView
import io.paytouch.core.data.driver.CustomPlainImplicits._

object QueryListBuilder {

  def apply[V <: ReportListView](filters: ReportListFilters[V], user: UserContext): QueryListBuilder[V] =
    new QueryListBuilder[V](filters, user)
}

class QueryListBuilder[V <: ReportListView](val filters: ReportListFilters[V], val user: UserContext)
    extends QueryBuilder {

  val queryType = ListQuery

  lazy val view = filters.view

  lazy val aggregationView = view.aggregationView(filters)

  override val query = defaultQuery.copy(
    withViews = defaultQuery.withViews ++ Seq(s"$listTable AS (${view.listView(filters)})"),
    tables = Seq(aggregationsTable, listTable),
    groupBy = Seq.empty,
  )

  lazy val listTable = "list_table"

  lazy val selectors: Seq[String] = {

    val listSelectors: Seq[String] =
      view.orderByEnum.orderedValuesWithExpandedFirst.map { v =>
        if (view.orderByEnum.alwaysExpanded.contains(v)) s"$listTable.${v.columnName}"
        else if (filters.fields.contains(v)) s"$aggregationsTable.${v.columnName}"
        else Null
      }

    val countSelectors = Seq(s"$aggregationsTable.cnt")

    countSelectors ++ listSelectors
  }

  lazy val groupBySelectors: Seq[String] = Seq.empty

  lazy val orderBySelectors: Seq[String] =
    filters.orderByFields.map(v => s"${v.columnName} ${v.ordering.entryName}")

  lazy val where: Seq[String] = {
    val idsWhere = filters
      .ids
      .map(ids => s"$aggregationsTable.id IN (${ids.asInParametersList})")
      .toSeq
    idsWhere ++ Seq(s"$listTable.id = $aggregationsTable.id")
  }

  lazy val limit = Some(filters.pagination.limit.toString)
  lazy val offset = Some(filters.pagination.offset.toString)
}
