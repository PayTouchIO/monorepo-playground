package io.paytouch.core.reports.entities

final case class ReportResponse[T](meta: ReportMetadata, data: Seq[ReportData[T]])
