package io.paytouch.core.reports.views

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelper
import io.paytouch.core.reports.entities.ReportData
import io.paytouch.core.reports.entities.enums.ops.{ ListOrderByFields, ListOrderByFieldsEnum }
import io.paytouch.core.reports.filters.{ ReportFilters, ReportListFilters }
import io.paytouch.core.reports.queries.{ EnrichResult, QueryListBuilder }
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

trait ReportListView extends ReportView {
  type OrderBy <: ListOrderByFieldsEnum
  type ListResult <: scala.Product

  lazy val idColumn = s"$tableNameAlias.id"

  val listTable: String

  def listResultConverter: CSVConverterHelper[ListResult]

  def orderByEnum: ListOrderByFields[OrderBy]

  protected def listResult(implicit user: UserContext): GetResult[Option[ListResult]]
  protected def enrichListResult(
      filters: ReportListFilters[_],
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ListResult]] =
    EnrichResult.identity

  // Used by QueryListBuilder to build query
  def aggregationView(filters: ReportListFilters[_]): String = {
    val selectors = Seq(s"$idColumn AS id", s"COUNT($tableNameAlias.*) AS cnt") ++ fieldSelectors(filters)
    val joins = defaultJoins(filters)
    val groupBys = Seq(idColumn) ++ groupByClauses(filters)

    innerQueryBuilder(selectors, joins, groupBys)
  }

  // Used by QueryListBuilder to build query
  def listView(filters: ReportFilters) = {
    val selectors = orderByEnum.alwaysExpanded.map(_.columnName)
    s"""SELECT ${selectors.mkString(", ")}
       |FROM $listTable
       |WHERE ${listWhereClauses(filters).mkString(" AND ")}""".stripMargin
  }

  // Used by QueryService to actually fetch data
  def countAllQuery(filters: ReportListFilters[_])(implicit db: Database, user: UserContext): DBIO[Int] = {
    val query =
      s"""SELECT COUNT(*)
         |FROM $listTable
         |WHERE ${listWhereClauses(filters).mkString(" AND ")}""".stripMargin
    sql"#$query".as[Int].head
  }

  // Used by QueryService to actually fetch data
  def listAllQuery[V <: ReportListView](
      filters: ReportListFilters[V],
    )(implicit
      db: Database,
      ec: ExecutionContext,
      user: UserContext,
    ): DBIO[Seq[ReportData[ListResult]]] = {
    implicit val getResult = listResult
    implicit val enrichResult = enrichListResult(filters)
    genericSQL(QueryListBuilder(filters, user))
  }

  protected def listWhereClauses(filters: ReportFilters): Seq[String]

  private def fieldSelectors(filters: ReportListFilters[_]): Seq[String] =
    customSelectors(orderByEnum.orderedValuesWithoutExpanded, filters)

  private def groupByClauses(filters: ReportListFilters[_]): Seq[String] =
    orderByEnum.orderedValuesWithoutExpanded.flatMap(f => f.groupBy)

}
