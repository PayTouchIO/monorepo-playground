package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class ReportFieldsOrderTaxRateAggregateCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[ReportFields[OrderTaxRateAggregate]]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[OrderTaxRateAggregate]] = reportResponseBuilder(
        Seq(ReportFields(Some("key"), OrderTaxRateAggregate(1, Some(10.129.$$$)))),
        Seq(ReportFields(Some("another_key"), OrderTaxRateAggregate(5, Some(13.124.$$$)))),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(orderTaxRateAggregateConverter))
      val filters = ReportAggrFilters[OrderTaxRateView](
        OrderTaxRateView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(OrderTaxRateGroupBy.Name),
        OrderTaxRateFields.values,
        None,
        None,
        UUID.randomUUID,
      )

      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List("Start Time", "End Time", "Name", "Count", "Tax Amount")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "1", "10.13")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "5", "13.12")
    }

    "convert instances of report response without count" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[OrderTaxRateAggregate]] = reportResponseBuilder(
        Seq(ReportFields(Some("key"), OrderTaxRateAggregate(1, Some(10.129.$$$)))),
        Seq(ReportFields(Some("another_key"), OrderTaxRateAggregate(5, Some(13.124.$$$)))),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(orderTaxRateAggregateConverter))
      val filters = ReportAggrFilters[OrderTaxRateView](
        OrderTaxRateView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(OrderTaxRateGroupBy.Name),
        OrderTaxRateFields.values.filterNot(_ == OrderTaxRateFields.Count),
        None,
        None,
        UUID.randomUUID,
      )

      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List("Start Time", "End Time", "Name", "Tax Amount")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "10.13")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "13.12")
    }
  }

}
