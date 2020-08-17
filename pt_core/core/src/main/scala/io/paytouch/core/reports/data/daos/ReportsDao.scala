package io.paytouch.core.reports.data.daos

import java.time.ZonedDateTime

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.utils.Formatters

import scala.concurrent._

trait ReportsDao extends LazyLogging {
  def db: Database
  implicit def ec: ExecutionContext

  protected def computeSQLFunc(sqlFuncOfOrderIds: String, filters: AdminReportFilters): Future[Int] = {
    val whereClauses = Seq(
      filters.ids.map(ids => s"orders.id IN (${ids.asInParametersList})"),
      filters.merchantIds.map(mIds => s"orders.merchant_id IN (${mIds.asInParametersList})"),
      filters.locationIds.map(lIds => s"orders.location_id IN (${lIds.asInParametersList})"),
      filters.from.map(date => s"orders.updated_at >= ${dateTimeAsString(date)}"),
      filters.to.map(date => s"orders.updated_at <= ${dateTimeAsString(date)}"),
      Some(
        "online_order_attributes is NULL OR online_order_attributes.acceptance_status != 'open'",
      ),
    ).flatten

    val ensuringPrecedence: String => String =
      clause => s"($clause)"

    val whereStatement =
      if (whereClauses.isEmpty)
        ""
      else
        whereClauses.map(ensuringPrecedence).mkString("WHERE ", " AND ", "")

    val query =
      s"""|SELECT $sqlFuncOfOrderIds(
          |  ARRAY(
          |    SELECT orders.id
          |    FROM orders LEFT JOIN online_order_attributes
          |    ON online_order_attributes.id = orders.online_order_attribute_id
          |    $whereStatement
          |  )
          |);""".stripMargin

    runQuery(query)
  }

  private def dateTimeAsString(date: ZonedDateTime): String = {
    val dateAsString = Formatters.ZonedDateTimeFormatter.format(date)
    s"'$dateAsString'"
  }

  private def runQuery[R](query: String): Future[Int] = {
    logger.debug(s"Update reports query: {}", query)
    db.run(sql"#$query".as[Int]).map(_.sum)
  }
}
