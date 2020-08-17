package io.paytouch.core.reports.entities

import io.paytouch.core.entities.PaginationLinks
import io.paytouch.core.reports.entities.enums.ReportInterval

final case class ReportMetadata(interval: Option[ReportInterval], pagination: Option[PaginationLinks])

object ReportMetadata {
  def apply(interval: ReportInterval, pagination: Option[PaginationLinks] = None): ReportMetadata =
    apply(Option(interval).filterNot(_ == ReportInterval.NoInterval), pagination)
}
