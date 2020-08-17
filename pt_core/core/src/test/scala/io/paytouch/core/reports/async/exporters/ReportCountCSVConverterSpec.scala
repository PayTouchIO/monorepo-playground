package io.paytouch.core.reports.async.exporters

import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class ReportCountCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for " should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportCount] = reportResponseBuilder(
        Seq(ReportCount(Some("key"), 5)),
        Seq(ReportCount(Some("another-key"), 10), ReportCount(Some("another-key"), 15)),
      )

      implicit val converterHelper = converterBuilder(reportCountConverter)
      val filters = ReportAggrFilters[OrderView](
        OrderView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(OrderGroupBy.PaymentType),
        Seq.empty,
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 3
      header ==== List("Start Time", "End Time", "Payment Type", "Count")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "5")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another-key", "10")
      rows(2) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another-key", "15")
    }

  }

}
