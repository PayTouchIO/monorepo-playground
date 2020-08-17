package io.paytouch.core.reports.async.exporters

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class GenericCSVConverterHelperSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper" should {

    "remove non expanded columns" in new CSVConverterHelperSpecContext {
      val entity: ReportResponse[ReportFields[OrderAggregate]] = reportResponseBuilder(
        Seq(ReportFields(None, OrderAggregate(1, None, Some(20.$$$), None))),
        Seq(ReportFields(None, OrderAggregate(5, None, Some(13.$$$), None))),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(orderAggregateConverter))
      val filters = ReportAggrFilters[OrderView](
        OrderView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        None,
        Seq(OrderFields.Count, OrderFields.Revenue),
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List("Start Time", "End Time", "Count", "Revenue")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "1", "20.00")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "5", "13.00")
    }
  }

}
