package io.paytouch.core.reports.entities

final case class SingleReportData[T](timeframe: ReportTimeframe, result: T)

object SingleReportData {
  def toSeqReportData[T](dataSeq: Seq[SingleReportData[Option[T]]]): Seq[ReportData[T]] = {
    val groupedData: Map[ReportTimeframe, Seq[T]] =
      dataSeq
        .groupBy(_.timeframe)
        .transform((_, v) => v.flatMap(_.result))

    val reports = groupedData.map { case (tf, r) => ReportData(tf, r) }.toSeq
    reports.sortBy(_.timeframe.start.toString)
  }

}
