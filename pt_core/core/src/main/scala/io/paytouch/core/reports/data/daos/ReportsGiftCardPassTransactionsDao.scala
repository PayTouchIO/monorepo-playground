package io.paytouch.core.reports.data.daos

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.reports.filters.AdminReportFilters

import scala.concurrent._

class ReportsGiftCardPassTransactionsDao(implicit val ec: ExecutionContext, val db: Database) extends ReportsDao {

  def compute(filters: AdminReportFilters): Future[Unit] =
    for {
      _ <- computeSQLFunc("reports_gift_card_passes_update", filters)
      _ <- computeSQLFunc("reports_gift_card_pass_transactions_update", filters)
    } yield ()

}
