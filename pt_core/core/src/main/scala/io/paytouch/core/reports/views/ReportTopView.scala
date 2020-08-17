package io.paytouch.core.reports.views

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelper
import io.paytouch.core.reports.entities.ReportData
import io.paytouch.core.reports.entities.enums.{ OrderByFields, OrderByFieldsEnum }
import io.paytouch.core.reports.filters.ReportTopFilters
import io.paytouch.core.reports.queries.{ EnrichResult, QueryTopBuilder }
import slick.dbio.DBIO
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

trait ReportTopView extends ReportView {
  type OrderBy <: OrderByFieldsEnum
  type TopResult <: scala.Product

  def topCSVConverter: CSVConverterHelper[TopResult]

  def topResult(implicit user: UserContext): GetResult[Option[TopResult]]
  def enrichTopResult(implicit user: UserContext): EnrichResult[Option[TopResult]] = EnrichResult.identity

  def orderByEnum: OrderByFields[OrderBy]

  def mainTopView(filters: ReportTopFilters[_]): String = {
    val selectors = fieldSelectors(filters)
    val joins = defaultJoins(filters)
    val groupBys = groupByClauses(filters)

    innerQueryBuilder(selectors, joins, groupBys)
  }

  private def fieldSelectors(filters: ReportTopFilters[_]): Seq[String] =
    customSelectors(orderByEnum.orderedValues, filters)

  private def groupByClauses(filters: ReportTopFilters[_]): Seq[String] =
    orderByEnum.orderedValues.flatMap(f => f.groupBy)

  def topQuery[V <: ReportTopView](
      filters: ReportTopFilters[V],
    )(implicit
      ec: ExecutionContext,
      user: UserContext,
    ): DBIO[Seq[ReportData[TopResult]]] = {
    implicit val getResult = topResult
    implicit val enrichResult = enrichTopResult
    genericSQL(QueryTopBuilder(filters, user))
  }
}
