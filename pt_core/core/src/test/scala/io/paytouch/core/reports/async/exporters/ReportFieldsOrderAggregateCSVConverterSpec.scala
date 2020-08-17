package io.paytouch.core.reports.async.exporters

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class ReportFieldsOrderAggregateCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[ReportFields[OrderAggregate]]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[OrderAggregate]] = reportResponseBuilder(
        Seq(ReportFields(Some("key"), OrderAggregate(1, Some(10.$$$), Some(20.$$$), Some(5)))),
        Seq(ReportFields(Some("another_key"), OrderAggregate(5, Some(13.$$$), None, None))),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(orderAggregateConverter))
      val filters = ReportAggrFilters[OrderView](
        OrderView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(OrderGroupBy.SourceType),
        OrderFields.values,
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List("Start Time", "End Time", "Source Type", "Count", "Revenue", "Profit", "Waiting Time in Seconds")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "1", "20.00", "10.00", "5")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "5", "", "13.00", "")
    }

    "convert instances of report response without count" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[OrderAggregate]] = reportResponseBuilder(
        Seq(ReportFields(Some("key"), OrderAggregate(1, Some(10.$$$), Some(20.$$$), Some(5)))),
        Seq(ReportFields(Some("another_key"), OrderAggregate(5, Some(13.$$$), None, None))),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(orderAggregateConverter))
      val filters = ReportAggrFilters[OrderView](
        OrderView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(OrderGroupBy.SourceType),
        OrderFields.values.filterNot(_ == OrderFields.Count),
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List("Start Time", "End Time", "Source Type", "Revenue", "Profit", "Waiting Time in Seconds")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "20.00", "10.00", "5")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "", "13.00", "")
    }
  }

}
