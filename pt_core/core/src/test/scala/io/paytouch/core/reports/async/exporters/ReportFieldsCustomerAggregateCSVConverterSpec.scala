package io.paytouch.core.reports.async.exporters

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class ReportFieldsCustomerAggregateCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[ReportFields[CustomerAggregate]]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[CustomerAggregate]] = reportResponseBuilder(
        Seq(ReportFields(Some("key"), CustomerAggregate(1, Some(10.$$$)))),
        Seq(
          ReportFields(Some("another_key"), CustomerAggregate(1, Some(13.$$$))),
          ReportFields(Some("another_another_key"), CustomerAggregate(5, None)),
        ),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(customerAggregateConverter))
      val filters = ReportAggrFilters[CustomerView](
        CustomerView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(CustomerGroupBy.Visit),
        CustomerFields.values,
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 3
      header ==== List("Start Time", "End Time", "Visit", "Count", "Spend")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "1", "10.00")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "1", "13.00")
      rows(2) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_another_key", "5", "")
    }

    "convert instances of report response without a count" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[CustomerAggregate]] = reportResponseBuilder(
        Seq(ReportFields(Some("key"), CustomerAggregate(1, Some(10.$$$)))),
        Seq(
          ReportFields(Some("another_key"), CustomerAggregate(1, Some(13.$$$))),
          ReportFields(Some("another_another_key"), CustomerAggregate(5, None)),
        ),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(customerAggregateConverter))
      val filters = ReportAggrFilters[CustomerView](
        CustomerView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(CustomerGroupBy.Visit),
        CustomerFields.values.filterNot(_ == CustomerFields.Count),
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 3
      header ==== List("Start Time", "End Time", "Visit", "Spend")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "10.00")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "13.00")
      rows(2) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_another_key", "")
    }
  }

}
