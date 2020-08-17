package io.paytouch.core.reports.entities

final case class ReportData[T](timeframe: ReportTimeframe, result: Seq[T])
