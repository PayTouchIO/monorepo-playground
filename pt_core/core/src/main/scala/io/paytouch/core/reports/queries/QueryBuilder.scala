package io.paytouch.core.reports.queries

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.views.ReportView

final case class Query(
    withViews: Seq[String],
    selectors: Seq[String],
    tables: Seq[String],
    joins: Seq[String],
    conditions: Seq[String],
    groupBy: Seq[String],
    orderBy: Seq[String],
    limit: Option[String],
    offset: Option[String],
  ) {

  def optionalPiece(
      keyword: String,
      data: Seq[String],
      sep: String,
    ) =
    if (data.isEmpty) "" else s"$keyword ${data.mkString(sep)}"

  val withViewsSql = optionalPiece("WITH", withViews, ",")
  val select = s"SELECT ${selectors.mkString(",")}"
  val from = s"FROM ${tables.mkString(",")}"
  val joinsSql = s"${joins.mkString(" ")}"
  val where = optionalPiece("WHERE", conditions, " AND ")
  val groupBySql = optionalPiece("GROUP BY", groupBy, ",")
  val orderBySql = optionalPiece("ORDER BY", orderBy, ",")
  val limitSql = limit.fold("")(v => s"LIMIT $v")
  val offsetSql = offset.fold("")(v => s"OFFSET $v")

  lazy val sql: String = s"$withViewsSql $select $from $where $joinsSql $groupBySql $orderBySql $limitSql $offsetSql"
}

trait QueryBuilder extends IntervalHelpers {

  def filters: ReportFilters
  def queryType: QueryType
  def user: UserContext

  protected def view: ReportView

  lazy val sql: String = query.sql

  val Count = "COUNT"
  val Sum = "SUM"
  val Average = "AVG"
  val Null = "null"

  val defaultQuery: Query =
    Query(
      withViews = Seq(s"$intervalSeries AS ($intervalsView)", s"$aggregationsTable AS ($aggregationView)"),
      selectors = commonSelectors ++ selectors,
      tables = Seq(aggregationsTable),
      joins = Seq.empty,
      conditions = where,
      groupBy = commonSelectors ++ groupBySelectors,
      orderBy = commonSelectors ++ orderBySelectors,
      limit = limit,
      offset = offset,
    )

  val query: Query = defaultQuery

  lazy val aggregationsTable = "aggregations_table"

  lazy val tables = Seq(aggregationsTable)

  private lazy val intervalsView: String = intervalsView(filters)

  private lazy val commonSelectors = Seq(intervalStartTime, intervalEndTime)

  def selectors: Seq[String]

  def aggregationView: String

  def where: Seq[String]

  def groupBySelectors: Seq[String]

  def orderBySelectors: Seq[String]

  def limit: Option[String]

  def offset: Option[String]

}
