package io.paytouch.core.reports.queries

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views.ReportAggrView
import io.paytouch.core.data.driver.CustomPlainImplicits._

object QueryAggrBuilder {

  def apply[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
      queryType: QueryAggrType,
      user: UserContext,
    ): QueryAggrBuilder[V] =
    new QueryAggrBuilder[V](filters, queryType, user)
}

class QueryAggrBuilder[V <: ReportAggrView](
    val filters: ReportAggrFilters[V],
    val queryType: QueryAggrType,
    val user: UserContext,
  ) extends QueryBuilder {

  lazy val view = filters.view

  lazy val listTable = "list_table"

  override val query = defaultQuery.copy(
    withViews = defaultQuery.withViews ++ view.expandView(filters).toSeq.map(expand => s"$listTable AS ($expand)"),
    tables = Seq(aggregationsTable) ++ view.expandView(filters).toSeq.map(_ => listTable),
    groupBy = if (view.groupByInOuterQuery) defaultQuery.groupBy ++ groupBySelectors else Seq.empty,
  )

  lazy val aggregationView = view.mainAggrView(filters, queryType)

  lazy val selectors: Seq[String] = {

    def aliasedColumn(
        field: V#Field,
        table: String,
        op: String,
      ) =
      if (view.groupByInOuterQuery) field.aggregatedSelector(op, table)
      else s"$table.${field.columnName} AS ${field.columnName}"

    def aggregatedSelectors(op: String): Seq[String] =
      view.fieldsEnum.orderedValuesWithExpandedFirst.map { v =>
        if (view.fieldsEnum.alwaysExpanded.contains(v)) aliasedColumn(v, listTable, op)
        else if (filters.fields.contains(v)) aliasedColumn(v, aggregationsTable, op)
        else s"$Null AS ${v.columnName}"
      }

    val countSelectors =
      if (view.groupByInOuterQuery) Seq(s"COUNT($aggregationsTable.id) AS cnt") else Seq(s"$aggregationsTable.cnt")

    val defaultSelectors: Seq[String] = queryType match {
      case CountQuery   => countSelectors
      case SumQuery     => countSelectors ++ aggregatedSelectors(Sum)
      case AverageQuery => countSelectors ++ aggregatedSelectors(Average)
    }

    val groupBySelectors = filters.groupBy match {
      case None    => Seq(Null)
      case Some(v) => Seq(v.columnName)
    }

    defaultSelectors ++ groupBySelectors
  }

  lazy val groupBySelectors: Seq[String] = orderBySelectors

  lazy val orderBySelectors: Seq[String] = (view.fieldsEnum.alwaysExpanded ++ filters.groupBy).map(_.columnName)

  lazy val where: Seq[String] = {
    val idsWhere = filters
      .ids
      .map(ids => s"$aggregationsTable.id IN (${ids.asInParametersList})")
      .toSeq
    idsWhere ++ view.expandView(filters).toSeq.map(_ => s"$listTable.id = $aggregationsTable.id")
  }

  lazy val limit = None
  lazy val offset = None
}
