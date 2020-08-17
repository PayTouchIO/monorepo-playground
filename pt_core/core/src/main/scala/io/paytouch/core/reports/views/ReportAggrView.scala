package io.paytouch.core.reports.views

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelper
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums.ops.{ CustomisableField, Fields, FieldsEnum, GroupByEnum }
import io.paytouch.core.reports.entities.{ ReportCount, ReportData, ReportFields }
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportFilters }
import io.paytouch.core.reports.queries._
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

trait ReportAggrView extends ReportView {
  type GroupBy <: GroupByEnum
  type Field <: FieldsEnum
  type AggrResult <: scala.Product

  protected def aggrCsvConverter: CSVConverterHelper[AggrResult]

  lazy val reportFieldsConverter = new ReportFieldsConverter[AggrResult](aggrCsvConverter)

  lazy val idColumn = s"$tableNameAlias.id"

  val mandatoryIdsFilter = false

  def aggrResult(implicit user: UserContext): GetResult[Option[ReportFields[AggrResult]]]
  def enrichAggrResult(
      filters: ReportAggrFilters[_],
      queryType: QueryAggrType,
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ReportFields[AggrResult]]] = EnrichResult.identity
  val fieldsEnum: Fields[Field]

  val countSelectors = Seq(s"$idColumn AS id", s"COUNT($tableNameAlias.id) AS cnt")
  val baseGroupBys = Seq(idColumn)

  def mainAggrView(filters: ReportAggrFilters[_], queryType: QueryAggrType): String = {
    val selectors = queryType match {
      case CountQuery   => countSelectors ++ fieldSelectors("COUNT", filters)
      case SumQuery     => countSelectors ++ fieldSelectors("SUM", filters)
      case AverageQuery => countSelectors ++ fieldSelectors("AVG", filters)
    }
    val joins = defaultJoins(filters)
    val groupBys = baseGroupBys ++ groupByClauses(filters)

    innerQueryBuilder(selectors, joins, groupBys)
  }

  def expandView(filters: ReportFilters): Option[String]

  val groupByInOuterQuery = false

  private def fieldSelectors(op: String, filters: ReportAggrFilters[_]): Seq[String] =
    aggregatedSelectors(op, allFields(filters), filters)

  def aggregatedSelectors(
      op: String,
      fields: Seq[CustomisableField],
      filters: ReportFilters,
    ): Seq[String] =
    fields.map { f =>
      val selector = f.selector(filters).getOrElse(f.aggregatedSelector(op, tableNameAlias))
      s"$selector AS ${f.columnName}"
    }

  private def groupByClauses(filters: ReportAggrFilters[_]): Seq[String] =
    filters.groupBy.toSeq.flatMap(f => f.groupBy)

  private def allFields(filters: ReportAggrFilters[_]): Seq[CustomisableField] =
    (filters
      .fields
      .filterNot(_.toIgnore)
      .filterNot(fieldsEnum.alwaysExpanded.contains) ++ filters.groupBy.toSeq).distinct

  def aggregationQuery[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
      queryType: QueryAggrType,
    )(implicit
      db: Database,
      ec: ExecutionContext,
      user: UserContext,
    ): DBIO[Seq[ReportData[ReportFields[AggrResult]]]] = {
    implicit val getResult = aggrResult
    implicit val enrichResult = enrichAggrResult(filters, queryType)
    genericSQL(QueryAggrBuilder(filters, queryType, user))
  }

  def aggregationCountQuery[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      db: Database,
      ec: ExecutionContext,
      user: UserContext,
    ): DBIO[Seq[ReportData[ReportCount]]] = {
    implicit val getResult = GetResult(r => Some(ReportCount(count = r.nextInt(), key = r.nextStringOption())))
    implicit val enrichResult = EnrichResult.identity[Option[ReportCount]]
    genericSQL(QueryAggrBuilder(filters, CountQuery, user))
  }
}
