package io.paytouch.core.reports.services

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.entities.AdminContext
import io.paytouch.core.reports.data.daos.{ ReportsGiftCardPassTransactionsDao, ReportsOrderDao }
import io.paytouch.core.reports.filters.AdminReportFilters

import scala.concurrent._

class AdminReportService(val db: Database)(implicit val ec: ExecutionContext) {
  implicit val d: Database = db
  private lazy val reportsOrderDao = new ReportsOrderDao
  private lazy val reportsGiftCardPassTransactionDao = new ReportsGiftCardPassTransactionsDao

  def adminRecomputeReports(filters: AdminReportFilters)(implicit admin: AdminContext): Future[Int] =
    triggerUpdateReports(filters)

  def triggerUpdateReports(filters: AdminReportFilters): Future[Int] =
    for {
      orders <- reportsOrderDao.compute(filters)
      _ <- reportsGiftCardPassTransactionDao.compute(filters)
    } yield orders

}
