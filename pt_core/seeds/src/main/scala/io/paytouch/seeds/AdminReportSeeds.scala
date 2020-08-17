package io.paytouch.seeds

import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.services.AdminReportService

import scala.concurrent._

object AdminReportSeeds extends Seeds {

  lazy val adminReportService = new AdminReportService(db)

  def load(implicit user: UserRecord): Future[Int] = {
    val filters = AdminReportFilters(ids = None, merchantIds = Some(Seq(user.merchantId)))
    adminReportService.triggerUpdateReports(filters = filters)
  }
}
