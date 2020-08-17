package io.paytouch.core.processors

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.messages.entities.{ OrderSynced, SQSMessage }
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.services.AdminReportService

class OrderSyncedProcessor(adminReportService: AdminReportService)(implicit val ec: ExecutionContext)
    extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: OrderSynced => updateReports(msg)
  }

  private def updateReports(msg: OrderSynced): Future[Unit] = {
    val filters = AdminReportFilters.fromMsg(msg)
    adminReportService.triggerUpdateReports(filters).void
  }
}
