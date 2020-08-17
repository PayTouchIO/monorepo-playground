package io.paytouch.core.reports.entities

import java.time.LocalDateTime

import io.paytouch.core.utils.Formatters

final case class ReportTimeframe(start: LocalDateTime, end: LocalDateTime)

object ReportTimeframe {

  def apply(start: String, end: String): ReportTimeframe =
    apply(asLocalDate(start), asLocalDate(end))

  private def asLocalDate(text: String): LocalDateTime = LocalDateTime.parse(text, Formatters.TimestampFormatter)
}
