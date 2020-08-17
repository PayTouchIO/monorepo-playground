package io.paytouch.core.reports.async.exporters

import java.util.UUID

import akka.http.scaladsl.server.RequestContext
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportFilters, ReportListFilters, ReportTopFilters }
import io.paytouch.core.reports.views.{ ReportAggrView, ReportListView, ReportTopView }

sealed trait ReportMsg {
  def exportId: UUID
  def filename: String
  def filters: ReportFilters
  def opsType: String
  def user: UserContext
}

final case class CountReport[V <: ReportAggrView](
    exportId: UUID,
    filename: String,
    filters: ReportAggrFilters[V],
    user: UserContext,
  ) extends ReportMsg {
  val opsType = "count"
}

final case class SumReport[V <: ReportAggrView](
    exportId: UUID,
    filename: String,
    filters: ReportAggrFilters[V],
    user: UserContext,
  ) extends ReportMsg {
  val opsType = "sum"
}

final case class AverageReport[V <: ReportAggrView](
    exportId: UUID,
    filename: String,
    filters: ReportAggrFilters[V],
    user: UserContext,
  ) extends ReportMsg {
  val opsType = "average"
}

final case class TopReport[V <: ReportTopView](
    exportId: UUID,
    filename: String,
    filters: ReportTopFilters[V],
    user: UserContext,
  ) extends ReportMsg {
  val opsType = "top"
}

final case class ListReport[V <: ReportListView](
    exportId: UUID,
    filename: String,
    filters: ReportListFilters[V],
    ctx: RequestContext,
    user: UserContext,
  ) extends ReportMsg {
  val opsType = "list"
}
