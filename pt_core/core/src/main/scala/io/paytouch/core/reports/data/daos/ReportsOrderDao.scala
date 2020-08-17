package io.paytouch.core.reports.data.daos

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.reports.filters.AdminReportFilters

import scala.concurrent._

class ReportsOrderDao(implicit val ec: ExecutionContext, val db: Database) extends ReportsDao {
  def compute(filters: AdminReportFilters): Future[Int] =
    computeSQLFunc("reports_orders_update", filters)
}
