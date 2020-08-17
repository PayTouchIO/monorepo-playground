package io.paytouch.core.reports.views

import java.time.{ LocalDateTime, ZoneId }
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities.enums.ops.CustomisableField
import io.paytouch.core.reports.entities.{ ReportData, ReportTimeframe, SingleReportData }
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.{ EnrichResult, IntervalHelpers, QueryBuilder }
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

trait ReportView extends IntervalHelpers with LazyLogging {

  def endpoint: String

  def tableName(filters: ReportFilters) = tableNameAlias
  lazy val tableNameAlias = endpoint

  def locationClauses(locationIds: Seq[UUID]): Seq[String]

  def dateClauses(
      from: LocalDateTime,
      to: LocalDateTime,
      timezone: ZoneId,
    ): Seq[String] = {
    val zonedFrom = localDateTimeAsString(from)
    val zonedTo = localDateTimeAsString(to)
    dateClauses(zonedFrom, zonedTo)
  }

  def dateClauses(from: String, to: String): Seq[String]

  def customSelectors(fields: Seq[CustomisableField], filters: ReportFilters): Seq[String] =
    fields.map { f =>
      val selector = f.selector(filters).getOrElse(s"$tableNameAlias.${f.columnName}")
      s"$selector AS ${f.columnName}"
    }

  def innerQueryBuilder(
      selectors: Seq[String],
      joins: Seq[String],
      groupBys: Seq[String],
    ): String =
    s"""SELECT ${(intervalSelectors ++ selectors).mkString(", ")}
       |FROM $intervalSeries
       |${joins.mkString(" ")}
       |GROUP BY ${(intervalSelectors ++ groupBys).mkString(", ")}
     """.stripMargin

  def defaultJoins(filters: ReportFilters): Seq[String]

  protected def genericSQL[T](
      queryBuilder: QueryBuilder,
    )(implicit
      getResult: GetResult[Option[T]],
      ec: ExecutionContext,
      enrichResult: EnrichResult[Option[T]],
      user: UserContext,
    ): DBIO[Seq[ReportData[T]]] = {
    val query = queryBuilder.sql
    logger.debug(s"Generated query: {}", query)
    for {
      singleReportDataSeq <- runAs(query)
      enrichedDataSeq <- DBIO.from(enrichResult(singleReportDataSeq))
    } yield SingleReportData.toSeqReportData(enrichedDataSeq)
  }

  private def runAs[R](generatedQuery: String)(implicit getResult: GetResult[R]): DBIO[Seq[SingleReportData[R]]] = {
    implicit val singleReportDataGetResult = GetResult { r =>
      val timeframe = ReportTimeframe(start = r.nextString(), end = r.nextString())
      val result = getResult.apply(r)
      SingleReportData(timeframe, result)
    }
    sql"#$generatedQuery".as[SingleReportData[R]]
  }
}
