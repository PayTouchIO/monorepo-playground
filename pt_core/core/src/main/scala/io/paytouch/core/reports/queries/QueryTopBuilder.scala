package io.paytouch.core.reports.queries

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.filters.ReportTopFilters
import io.paytouch.core.reports.views.ReportTopView

object QueryTopBuilder {

  def apply[V <: ReportTopView](filters: ReportTopFilters[V], user: UserContext): QueryTopBuilder[V] =
    new QueryTopBuilder[V](filters, user)
}

class QueryTopBuilder[V <: ReportTopView](val filters: ReportTopFilters[V], val user: UserContext)
    extends QueryBuilder {

  val queryType = TopQuery

  lazy val view = filters.view

  lazy val aggregationView = view.mainTopView(filters)

  lazy val selectors: Seq[String] = view.orderByEnum.orderedValues.map(_.columnName)

  lazy val groupBySelectors: Seq[String] = view.orderByEnum.orderedValues.map(_.columnName)

  lazy val orderBySelectors: Seq[String] =
    filters.orderByFields.map(v => s"${v.columnName} ${v.ordering.entryName}")

  lazy val where = Seq.empty[String]

  lazy val limit = Some(filters.n.toString)

  lazy val offset = None

}
